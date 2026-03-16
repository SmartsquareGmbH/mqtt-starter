package de.smartsquare.starter.mqtt.mapper

import tools.jackson.databind.json.JsonMapper

/**
 * Jackson implementation of [MqttObjectMapper].
 */
class JacksonMqttObjectMapper(private val jsonMapper: JsonMapper) : MqttObjectMapper {
    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any = jsonMapper.readValue(bytes, targetType)
    override fun toBytes(value: Any): String = jsonMapper.writeValueAsString(value)
}
