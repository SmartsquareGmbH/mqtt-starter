package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.hivemq.client.mqtt.datatypes.MqttTopic

/**
 * Class responsible for adapting from and to mqtt messages.
 */
interface MqttMessageAdapter {

    /**
     * Converts the given [message] into the expected [targetType].
     */
    fun adapt(message: MqttPublishContainer, targetType: Class<*>): Any

    /**
     * Converts the given [payload] into a [ByteArray].
     */
    fun adapt(payload: Any): ByteArray
}

/**
 * Adapter class with methods for converting from and to mqtt payloads.
 */
open class DefaultMqttMessageAdapter(private val objectMapper: ObjectMapper) : MqttMessageAdapter {

    /**
     * Converts the given [message] into the expected [targetType].
     */
    override fun adapt(message: MqttPublishContainer, targetType: Class<*>): Any {
        return when {
            targetType.isAssignableFrom(MqttTopic::class.java) -> message.topic
            targetType.isAssignableFrom(ByteArray::class.java) -> message.payload
            targetType.isAssignableFrom(String::class.java) -> message.payload.decodeToString()
            targetType.isAssignableFrom(message.value.javaClass) -> message.value
            else -> objectMapper.readValue(message.payload, targetType)
        }
    }

    /**
     * Converts the given [payload] into a [ByteArray].
     *
     * Strings and primitives are converted directly, other types are serialized to json.
     */
    override fun adapt(payload: Any): ByteArray {
        return when (payload) {
            is ByteArray -> payload
            is String -> payload.encodeToByteArray()
            else -> objectMapper.writeValueAsString(payload).encodeToByteArray()
        }
    }
}
