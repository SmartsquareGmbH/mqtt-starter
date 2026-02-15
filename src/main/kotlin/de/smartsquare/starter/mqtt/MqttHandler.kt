package de.smartsquare.starter.mqtt

import org.slf4j.LoggerFactory

/**
 * Class for consuming and forwarding messages to the correct subscriber.
 */
class MqttHandler(
    private val registry: MqttSubscriberRegistry,
    private val adapter: MqttMessageAdapter,
    private val messageErrorHandler: MqttMessageErrorHandler,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Handles a single [message]. The topic of the message is used to determine the correct subscriber which is then
     * invoked with parameters produced by the [MqttMessageAdapter].
     */
    fun handle(message: MqttPublishContainer) {
        val (topic, payload) = message

        logger.trace("Received mqtt message on topic [{}] with payload {}", topic, payload)

        val subscriber = registry.getSubscriber(topic) ?: error("No subscriber found for topic $topic")

        try {
            val parameters = subscriber.parameterTypes.map { adapter.adapt(message, it) }

            subscriber.delegate.invoke(*parameters.toTypedArray())
        } catch (error: Exception) {
            messageErrorHandler.handle(
                MqttMessageException(topic, payload, "Error while handling mqtt message on topic [$topic]", error),
            )
        }
    }
}
