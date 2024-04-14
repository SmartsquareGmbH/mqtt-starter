package de.smartsquare.starter.mqtt

import com.hivemq.client.internal.mqtt.message.publish.MqttPublish
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import java.util.concurrent.CompletableFuture

/**
 * Class with convenience methods for publishing a message on the mqtt broker.
 */
class Mqtt3Publisher(private val adapter: MqttMessageAdapter, client: Mqtt3Client) {

    private val asyncClient = client.toAsync()

    /**
     * Publishes the given [payload] on [topic] with quality of service level [qos].
     * Returns a [CompletableFuture] that is completed once the broker has accepted the message.
     */
    @JvmOverloads
    fun publish(topic: String, qos: MqttQos, payload: Any, retain: Boolean = false): CompletableFuture<Mqtt3Publish> {
        return asyncClient.publish(
            Mqtt3Publish.builder()
                .topic(topic)
                .qos(qos)
                .payload(adapter.adapt(payload))
                .retain(retain)
                .build(),
        )
    }
}

/**
 * Class with convenience methods for publishing a message on the mqtt broker.
 */
class Mqtt5Publisher(private val adapter: MqttMessageAdapter, client: Mqtt5Client) {

    private val asyncClient = client.toAsync()

    /**
     * Options for publishing a message.
     */
    data class PublishingOptions(
        val retain: Boolean = false,
        val messageExpiryInterval: Long = MqttPublish.NO_MESSAGE_EXPIRY,
        val payloadFormatIndicator: Mqtt5PayloadFormatIndicator? = null,
        val contentType: String? = null,
        val responseTopic: MqttTopic? = null,
        val correlationData: ByteArray? = null,
        val userProperties: Mqtt5UserProperties = Mqtt5UserProperties.of(),
    ) {
        companion object {
            /**
             * Creates a new [Builder] for [PublishingOptions]. With the given [original] as a base.
             * If no base is given, the default values from [PublishingOptions] are used.
             */
            @JvmStatic
            @JvmOverloads
            fun builder(original: PublishingOptions = PublishingOptions()) = object : Builder {
                private var state = original

                override fun retain(retain: Boolean): Builder = apply {
                    state = state.copy(retain = retain)
                }

                override fun messageExpiryInterval(messageExpiryInterval: Long): Builder = apply {
                    state = state.copy(messageExpiryInterval = messageExpiryInterval)
                }

                override fun payloadFormatIndicator(payloadFormatIndicator: Mqtt5PayloadFormatIndicator?): Builder =
                    apply { state = state.copy(payloadFormatIndicator = payloadFormatIndicator) }

                override fun contentType(contentType: String?): Builder = apply {
                    state = state.copy(contentType = contentType)
                }

                override fun responseTopic(responseTopic: MqttTopic?): Builder = apply {
                    state = state.copy(responseTopic = responseTopic)
                }

                override fun correlationData(correlationData: ByteArray?): Builder = apply {
                    state = state.copy(correlationData = correlationData)
                }

                override fun userProperties(userProperties: Mqtt5UserProperties): Builder = apply {
                    state = state.copy(userProperties = userProperties)
                }

                override fun build(): PublishingOptions = state
            }
        }

        /**
         * Fluent builder for [PublishingOptions]. It is initialized with the default values from [PublishingOptions].
         */
        interface Builder {
            fun retain(retain: Boolean): Builder
            fun messageExpiryInterval(messageExpiryInterval: Long): Builder
            fun payloadFormatIndicator(payloadFormatIndicator: Mqtt5PayloadFormatIndicator?): Builder
            fun contentType(contentType: String?): Builder
            fun responseTopic(responseTopic: MqttTopic?): Builder
            fun correlationData(correlationData: ByteArray?): Builder
            fun userProperties(userProperties: Mqtt5UserProperties): Builder
            fun build(): PublishingOptions
        }
    }

    /**
     * Publishes the given [payload] on [topic] with quality of service level [qos].
     * Returns a [CompletableFuture] that is completed once the broker has accepted the message.
     */
    @JvmOverloads
    fun publish(
        topic: String,
        qos: MqttQos,
        payload: Any,
        options: PublishingOptions? = null,
    ): CompletableFuture<Mqtt5PublishResult> {
        val build = Mqtt5Publish.builder().topic(topic).qos(qos).payload(adapter.adapt(payload))

        if (options != null) {
            build.retain(options.retain)
                .messageExpiryInterval(options.messageExpiryInterval)
                .payloadFormatIndicator(options.payloadFormatIndicator)
                .contentType(options.contentType)
                .responseTopic(options.responseTopic)
                .correlationData(options.correlationData)
                .userProperties(options.userProperties)
        }

        return asyncClient.publish(build.build())
    }
}
