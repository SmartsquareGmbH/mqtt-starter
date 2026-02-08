package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttTopic
import de.smartsquare.starter.mqtt.mapper.MqttObjectMapper
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Adapter class with methods for converting from and to mqtt payloads.
 */
open class MqttMessageAdapter(private val mqttObjectMapper: MqttObjectMapper) {

    /**
     * Converts the given [message] into the expected [targetType].
     */
    open fun adapt(message: MqttPublishContainer, targetType: Class<*>) = when {
        targetType.isAssignableFrom(MqttTopic::class.java) -> message.topic
        targetType.isAssignableFrom(ByteArray::class.java) -> message.payload
        targetType.isAssignableFrom(String::class.java) -> message.payload.decodeToString()
        targetType.isAssignableFrom(Int::class.java) -> message.payload.decodeToString().toInt()
        targetType.isAssignableFrom(Long::class.java) -> message.payload.decodeToString().toLong()
        targetType.isAssignableFrom(Float::class.java) -> message.payload.decodeToString().toFloat()
        targetType.isAssignableFrom(Double::class.java) -> message.payload.decodeToString().toDouble()
        targetType.isAssignableFrom(BigInteger::class.java) -> message.payload.decodeToString().toBigInteger()
        targetType.isAssignableFrom(BigDecimal::class.java) -> message.payload.decodeToString().toBigDecimal()
        targetType.isAssignableFrom(Boolean::class.java) -> message.payload.decodeToString().toBoolean()
        targetType.isAssignableFrom(message.value.javaClass) -> message.value
        else -> mqttObjectMapper.fromBytes(message.payload, targetType)
    }

    /**
     * Converts the given [payload] into a [ByteArray].
     *
     * Strings and primitives are converted directly, other types are serialized to JSON.
     */
    open fun adapt(payload: Any) = when (payload) {
        is ByteArray -> payload
        is String -> payload.encodeToByteArray()
        is Number -> payload.toString().encodeToByteArray()
        is Boolean -> payload.toString().encodeToByteArray()
        else -> mqttObjectMapper.toBytes(payload).encodeToByteArray()
    }
}
