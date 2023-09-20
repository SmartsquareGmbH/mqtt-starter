package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttTopic
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.lang.reflect.Method

/**
 * Helper class to find all beans with methods annotated with [MqttSubscribe].
 */
class AnnotationCollector : BeanPostProcessor {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * MultiMap of beans to its methods annotated with [MqttSubscribe] and the annotation itself.
     */
    val subscribers: MultiValueMap<Any, ResolvedMqttSubscriber> = LinkedMultiValueMap()

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        val collectedSubscribers = bean.javaClass.methods
            .mapNotNull { method ->
                val annotation = AnnotationUtils.findAnnotation(method, MqttSubscribe::class.java)

                annotation?.let { ResolvedMqttSubscriber(method, annotation) }
            }
            .sortedBy { (method) -> method.name }

        val erroneousSubscriberDefinitions = collectedSubscribers.filter { (method) -> method.isInvalidSignature() }

        if (erroneousSubscriberDefinitions.isNotEmpty()) {
            val joinedSubscribers = erroneousSubscriberDefinitions.joinToString(separator = ", ") { (method) ->
                "$beanName#${method.name}"
            }

            throw MqttConfigurationException(
                """Following subscribers are invalid [$joinedSubscribers].
                   Functions annotated with 'MqttSubscriber' can only have the following parameters:
                     - the MqttTopic
                     - any type of deserialized JSON payload
                     - or both
                """.trimMargin(),
            )
        }

        for (subscriber in collectedSubscribers) {
            logger.debug("Found subscriber ${subscriber.method.name} of ${bean.javaClass.simpleName}.")

            subscribers.add(bean, subscriber)
        }

        return bean
    }

    /**
     * Returns true if the method has defined more than one parameter besides the topic.
     */
    private fun Method.isInvalidSignature(): Boolean {
        val topicParamCount = this.parameterTypes.count { it.isAssignableFrom(MqttTopic::class.java) }
        val payloadParamCount = this.parameterTypes.count { !it.isAssignableFrom(MqttTopic::class.java) }

        return topicParamCount > 1 || payloadParamCount > 1
    }

    data class ResolvedMqttSubscriber(val method: Method, val config: MqttSubscribe)
}
