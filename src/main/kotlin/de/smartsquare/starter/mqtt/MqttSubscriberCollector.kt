package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Method

/**
 * Helper class to find all beans with methods annotated with [MqttSubscribe].
 */
class MqttSubscriberCollector(configProvider: ObjectProvider<MqttProperties>) : BeanPostProcessor {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     *  It's really important to use lazy initialization here, because the bean value inside this provider is not
     *  available during construction time and only requested lazy up on the first found subscriber.
     *  At this point, the bean is already available and can be used.
     *  We cache the result here to avoid multiple expensive lookups from the underlying bean factory.
     */
    private val config: MqttProperties by lazy { configProvider.`object` }

    /**
     * MultiMap of beans to its methods annotated with [MqttSubscribe] and the annotation itself.
     */
    val subscribers: List<ResolvedMqttSubscriber>
        get() = _subscribers

    private val _subscribers = mutableListOf<ResolvedMqttSubscriber>()

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        val collectedSubscribers = bean.javaClass.methods
            .mapNotNull { method ->
                AnnotationUtils.findAnnotation(method, MqttSubscribe::class.java)?.let { annotation ->
                    val topic = if (annotation.shared && config.group != null) {
                        MqttTopicFilter.of("\$share/${config.group}/${annotation.topic}")
                    } else {
                        MqttTopicFilter.of(annotation.topic)
                    }

                    ResolvedMqttSubscriber(bean, method, topic, annotation.qos)
                }
            }
            .sortedBy { it.method.name }

        if (logger.isDebugEnabled) {
            for (subscriber in collectedSubscribers.reversed()) {
                logger.debug("Found subscriber ${subscriber.method.name} of $beanName.")
            }
        }

        _subscribers.addAll(collectedSubscribers)

        return bean
    }

    /**
     * Data class representing a subscriber method annotated with [MqttSubscribe].
     */
    data class ResolvedMqttSubscriber(val bean: Any, val method: Method, val topic: MqttTopicFilter, val qos: MqttQos)
}
