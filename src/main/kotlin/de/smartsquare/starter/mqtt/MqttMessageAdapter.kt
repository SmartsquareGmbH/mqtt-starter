package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.hivemq.client.mqtt.datatypes.MqttTopic

/**
 * Adapter class with methods for converting from and to mqtt payloads.
 */
class MqttMessageAdapter(private val objectMapper: ObjectMapper) {

    /**
     * Converts the given [topic] and [payload] into the expected [targetType].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> adapt(topic: MqttTopic, payload: ByteArray, targetType: Class<T>): T {
        return when {
            targetType.isAssignableFrom(MqttTopic::class.java) -> topic as T
            targetType.isAssignableFrom(String::class.java) -> payload.decodeToString() as T
            else -> objectMapper.readValue(payload, targetType)
        }
    }

    /**
     * Converts the given [payload] into a [ByteArray].
     *
     * Strings and primitives are converted directly, other types are serialized to json.
     */
    fun adapt(payload: Any): ByteArray {
        return when (payload) {
            is String -> payload.encodeToByteArray()
            else -> objectMapper.writeValueAsString(payload).encodeToByteArray()
        }
    }
}
