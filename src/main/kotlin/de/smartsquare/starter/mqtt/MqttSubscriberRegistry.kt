package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.getBeansOfType
import org.springframework.core.KotlinDetector
import org.springframework.core.annotation.AnnotationUtils
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Helper class to find all beans with methods annotated with [MqttSubscribe].
 *
 * Uses [SmartInitializingSingleton] to scan all beans after they have been fully initialized,
 * ensuring all dependencies are available during the collection process.
 */
class MqttSubscriberRegistry(private val beanFactory: ListableBeanFactory, private val config: MqttProperties) :
    SmartInitializingSingleton {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * List of topic filters to their resolved subscribers.
     */
    lateinit var subscribers: List<ResolvedMqttSubscriber>
        private set

    override fun afterSingletonsInstantiated() {
        subscribers = beanFactory.getBeansOfType<Any>()
            .flatMap { (beanName, bean) ->
                bean.javaClass.methods.mapNotNull { method -> processBeanMethod(bean, beanName, method) }
            }
            .sortedWith(
                compareBy<ResolvedMqttSubscriber> { it.topic.containsWildcards() }
                    .thenByDescending { it.topic.levels.size }
                    .thenByDescending { it.topic.toString().length },
            )
    }

    private fun processBeanMethod(bean: Any?, beanName: String, method: Method): ResolvedMqttSubscriber? {
        val annotation = AnnotationUtils.findAnnotation(method, MqttSubscribe::class.java) ?: return null

        val topic = if (annotation.shared && config.group != null) {
            MqttTopicFilter.of($$"$share/$${config.group}/$${annotation.topic}")
        } else {
            MqttTopicFilter.of(annotation.topic)
        }

        val isSuspend = KotlinDetector.isSuspendingFunction(method)

        val parameterTypes = if (isSuspend) {
            method.parameterTypes.dropLast(1)
        } else {
            method.parameterTypes.toList()
        }

        val delegate = if (isSuspend) {
            MqttAnnotatedMethodDelegate { args ->
                runBlocking {
                    suspendCoroutineUninterceptedOrReturn { continuation -> method.invoke(bean, *args, continuation) }
                }
            }
        } else {
            val handle = MethodHandles.publicLookup().unreflect(method)
            MqttAnnotatedMethodDelegate { args -> handle.invokeWithArguments(bean, *args) }
        }

        logger.debug("Found subscriber ${method.name} of $beanName.")

        return ResolvedMqttSubscriber(topic, annotation.qos, delegate, parameterTypes)
    }

    /**
     * Returns the subscriber for the given [topic].
     * If no subscriber is found, an error is thrown.
     */
    fun getSubscriber(topic: MqttTopic) = subscribers.find { it.topic.matches(topic) }

    /**
     * Delegate interface for invoking annotated subscriber methods.
     */
    fun interface MqttAnnotatedMethodDelegate {
        fun invoke(vararg args: Any)
    }

    /**
     * Data class representing a subscriber method annotated with [MqttSubscribe].
     */
    data class ResolvedMqttSubscriber(
        val topic: MqttTopicFilter,
        val qos: MqttQos,
        val delegate: MqttAnnotatedMethodDelegate,
        val parameterTypes: List<Class<*>>,
    )
}
