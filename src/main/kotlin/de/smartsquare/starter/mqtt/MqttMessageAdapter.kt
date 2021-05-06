package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish

class MqttMessageAdapter(private val objectMapper: ObjectMapper) {

    fun adapt(message: Mqtt3Publish, targetType: Class<*>): Any {
        return when {
            targetType.isAssignableFrom(MqttTopic::class.java) -> message.topic
            targetType.isAssignableFrom(String::class.java) -> message.payloadAsBytes.decodeToString()
            else -> objectMapper.readValue(message.payloadAsBytes, targetType)
        }
    }

    fun adapt(payload: Any): ByteArray {
        return when (payload) {
            is String -> payload.encodeToByteArray()
            else -> objectMapper.writeValueAsString(payload).encodeToByteArray()
        }
    }
}
