package de.smartsquare.smartbot.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component

@Component
class Adapter(
  private val collector: AnnotationCollector,
  private val jackson: ObjectMapper,
  private val client: Mqtt3Client
) : InitializingBean {

    private val asyncClient = client.toAsync()

    override fun afterPropertiesSet() {
        for ((bean, subscribers) in collector.subscribers) {
            for (subscriber in subscribers) {
                val payloadType = subscriber.parameterTypes.first()
                val annotation = subscriber.getAnnotation(MqttSubscribe::class.java)
                val topic = if (annotation.shared) "\$share/smartbot/${annotation.topic}" else annotation.topic

                asyncClient.subscribeWith()
                    .topicFilter(topic)
                    .qos(annotation.qos)
                    .callback { msg -> subscriber.invoke(bean, jackson.readValue(msg.payloadAsBytes, payloadType)) }
                    .send()
            }
        }
    }
}
