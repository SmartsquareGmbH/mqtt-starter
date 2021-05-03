package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class MqttPublisher(private val adapter: MqttMessageAdapter, client: Mqtt3Client) {

    private val asyncClient = client.toAsync()

    fun publish(topic: String, qos: MqttQos, payload: Any): CompletableFuture<Mqtt3Publish> {
        return asyncClient.publish(
            Mqtt3Publish.builder()
                .topic(topic)
                .qos(qos)
                .payload(adapter.adapt(payload))
                .build()
        )
    }
}
