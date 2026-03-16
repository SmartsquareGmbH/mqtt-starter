package de.smartsquare.starter.mqtt.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.ObjectProvider

/**
 * Jackson 2 implementation of [MqttObjectMapper].
 */
class Jackson2MqttObjectMapper(provider: ObjectProvider<ObjectMapper>) : MqttObjectMapper {
    private val objectMapper by lazy { provider.getObject() }

    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any = objectMapper.readValue(bytes, targetType)
    override fun toBytes(value: Any): String = objectMapper.writeValueAsString(value)
}
