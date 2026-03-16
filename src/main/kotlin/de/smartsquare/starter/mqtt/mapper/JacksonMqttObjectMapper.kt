package de.smartsquare.starter.mqtt.mapper

import org.springframework.beans.factory.ObjectProvider
import tools.jackson.databind.json.JsonMapper

/**
 * Jackson implementation of [MqttObjectMapper].
 */
class JacksonMqttObjectMapper(provider: ObjectProvider<JsonMapper>) : MqttObjectMapper {
    private val jsonMapper by lazy { provider.getObject() }

    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any = jsonMapper.readValue(bytes, targetType)
    override fun toBytes(value: Any): String = jsonMapper.writeValueAsString(value)
}
