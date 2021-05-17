package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttTopic
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import java.lang.reflect.Method

/**
 * Helper class to find all beans with methods annotated with [MqttSubscribe].
 */
class AnnotationCollector : BeanPostProcessor {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Map of beans to it's methods annotated with [MqttSubscribe].
     */
    val subscribers: Map<Any, List<Method>> get() = _subscribers
    private val _subscribers = mutableMapOf<Any, List<Method>>()

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        val collectedSubscribers = bean.javaClass.methods
            .filter { it.isAnnotationPresent(MqttSubscribe::class.java) }
            .sortedBy { it.name }

        val erroneousSubscriberDefinitions = collectedSubscribers.filter { it.isInvalidSignature() }

        if (erroneousSubscriberDefinitions.isNotEmpty()) {
            val joinedSubscribers = erroneousSubscriberDefinitions.joinToString(separator = ", ") {
                "$beanName#${it.name}"
            }

            throw MqttConfigurationException(
                """Following subscribers are invalid [$joinedSubscribers].
                   Functions annotated with 'MqttSubscriber' can only have the following parameters:
                     - the MqttTopic
                     - any type of deserialized JSON payload
                     - or both
                """.trimMargin()
            )
        }

        for (subscriber in collectedSubscribers) {
            logger.debug("Found subscriber ${subscriber.name} of ${bean.javaClass.simpleName}.")

            _subscribers[bean] = subscribers.getOrDefault(bean, emptyList()) + listOf(subscriber)
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
}
