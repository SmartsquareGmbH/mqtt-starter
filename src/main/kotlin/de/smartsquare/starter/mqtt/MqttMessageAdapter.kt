package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish

/**
 * Adapter class with methods for converting from and to mqtt payloads.
 */
class MqttMessageAdapter(private val objectMapper: ObjectMapper) {

    /**
     * Converts the given [message] into the expected [targetType].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> adapt(message: Mqtt3Publish, targetType: Class<T>): T {
        return when {
            targetType.isAssignableFrom(MqttTopic::class.java) -> message.topic as T
            targetType.isAssignableFrom(String::class.java) -> message.payloadAsBytes.decodeToString() as T
            else -> objectMapper.readValue(message.payloadAsBytes, targetType)
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
