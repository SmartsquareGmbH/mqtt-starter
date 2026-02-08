package de.smartsquare.starter.mqtt.mapper

import org.slf4j.LoggerFactory

/**
 * Fallback implementation of [MqttObjectMapper] that throws an error when used.
 */
class ErrorMqttObjectMapper : MqttObjectMapper {

    private companion object {
        private const val ERROR_MESSAGE =
            "No MqttObjectMapper available. Complex payloads will not be supported. " +
                "Configure a library for JSON support (e.g. Jackson) or implement a custom MqttObjectMapper."
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.warn(ERROR_MESSAGE)
    }

    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any {
        error(ERROR_MESSAGE)
    }

    override fun toBytes(value: Any): String {
        error(ERROR_MESSAGE)
    }
}
