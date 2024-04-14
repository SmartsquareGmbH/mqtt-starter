package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish

/**
 * Common interface for classes wrapping the Mqtt*Publish classes of the mqtt client.
 */
sealed interface MqttPublishContainer {
    val topic: MqttTopic

    val payload: ByteArray

    val value: Any

    @JvmSynthetic
    operator fun component1(): MqttTopic = topic

    @JvmSynthetic
    operator fun component2(): ByteArray = payload
}

/**
 * Container class for [Mqtt3Publish] instances. Used for common implementations like the [MqttMessageAdapter].
 */
@JvmInline
value class Mqtt3PublishContainer(override val value: Mqtt3Publish) : MqttPublishContainer {
    override val topic: MqttTopic get() = value.topic
    override val payload: ByteArray get() = value.payloadAsBytes
}

/**
 * Container class for [Mqtt5Publish] instances. Used for common implementations like the [MqttMessageAdapter].
 */
@JvmInline
internal value class Mqtt5PublishContainer(override val value: Mqtt5Publish) : MqttPublishContainer {
    override val topic: MqttTopic get() = value.topic
    override val payload: ByteArray get() = value.payloadAsBytes
}
