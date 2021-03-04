package de.smartsquare.smartbot.starter.mqtt

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import java.lang.reflect.Method

@Component
class AnnotationCollector : BeanPostProcessor {

    private val logger = LoggerFactory.getLogger(javaClass)

    val subscribers: MutableMap<Any, List<Method>> = mutableMapOf()

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        val collectedSubscribers = bean.javaClass.methods.filter { it.isAnnotationPresent(MqttSubscribe::class.java) }

        val erroneousSubscriberDefinitions = collectedSubscribers.filter { it.parameterCount != 1 }
        if (erroneousSubscriberDefinitions.size == 1) {
            val subscriber = erroneousSubscriberDefinitions.first().name

            throw SmartbotConfigurationException("Subscriber $subscriber should have exactly one parameter.")
        } else if (erroneousSubscriberDefinitions.size > 1) {
            val joinedSubscribers = erroneousSubscriberDefinitions.joinToString(separator = ", ") { it.name }

            throw SmartbotConfigurationException("Subscriber [$joinedSubscribers] should have exactly one parameter.")
        }

        for (subscriber in collectedSubscribers) {
            logger.debug("Found subscriber ${subscriber.name} of ${bean.javaClass.simpleName}.")

            subscribers[bean] = subscribers.getOrDefault(bean, emptyList()) + listOf(subscriber)
        }

        return bean
    }
}
