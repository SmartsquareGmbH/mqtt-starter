package de.smartsquare.starter.mqtt.mapper

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Jackson implementation of [MqttObjectMapper].
 */
class JacksonMqttObjectMapper(private val objectMapper: ObjectMapper) : MqttObjectMapper {
    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any = objectMapper.readValue(bytes, targetType)
    override fun toBytes(value: Any): String = objectMapper.writeValueAsString(value)
}
