package de.smartsquare.starter.mqtt

import org.slf4j.LoggerFactory

/**
 * Class responsible for handling errors happening during message delivery.
 * Writes logs per default but can be overridden by consumers.
 */
open class MqttMessageErrorHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Handles the given [error] that occurred during the delivery of an MQTT message.
     *
     * @param error The [MqttMessageException] representing the error during message delivery.
     */
    open fun handle(error: MqttMessageException) {
        logger.error("Error while delivering mqtt message on topic [${error.topic}]", error)
    }
}
