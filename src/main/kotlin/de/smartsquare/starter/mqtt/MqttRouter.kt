package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class MqttRouter(
    private val collector: AnnotationCollector,
    private val adapter: MqttMessageAdapter,
    private val config: MqttProperties,
    client: Mqtt3Client
) : InitializingBean {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val asyncClient = client.toAsync()

    override fun afterPropertiesSet() {
        for ((bean, subscribers) in collector.subscribers) {
            for (subscriber in subscribers) {
                val annotation = subscriber.getAnnotation(MqttSubscribe::class.java)
                val topic = if (annotation.shared && config.group != null) {
                    "\$share/${config.group}/${annotation.topic}"
                } else {
                    annotation.topic
                }

                asyncClient.subscribeWith()
                    .topicFilter(topic)
                    .qos(annotation.qos)
                    .callback { message -> deliver(subscriber, bean, message) }
                    .send()
            }
        }
    }

    private fun deliver(subscriber: Method, bean: Any, message: Mqtt3Publish) {
        try {
            val parameters = subscriber.parameterTypes.map { adapter.adapt(message, it) }.toTypedArray()

            subscriber.invoke(bean, *parameters)
        } catch (e: InvocationTargetException) {
            logger.error("Error while delivering mqtt message.", e)
        }
    }
}
