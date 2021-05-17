package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.util.concurrent.CompletableFuture

/**
 * Class with convenience methods for publishing a message on the mqtt broker.
 */
class MqttPublisher(private val adapter: MqttMessageAdapter, client: Mqtt3Client) {

    private val asyncClient = client.toAsync()

    /**
     * Publishes the given [payload] on [topic] with quality of service level [qos].
     * Returns a [CompletableFuture] that is completed once the broker has accepted the message.
     */
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
