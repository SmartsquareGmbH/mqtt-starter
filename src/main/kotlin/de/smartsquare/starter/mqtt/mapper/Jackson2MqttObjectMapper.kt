package de.smartsquare.starter.mqtt.mapper

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Jackson 2 implementation of [MqttObjectMapper].
 */
class Jackson2MqttObjectMapper(private val objectMapper: ObjectMapper) : MqttObjectMapper {
    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any = objectMapper.readValue(bytes, targetType)
    override fun toBytes(value: Any): String = objectMapper.writeValueAsString(value)
}
