package de.smartsquare.starter.mqtt.mapper

import com.google.gson.Gson

/**
 * Gson implementation of [MqttObjectMapper].
 */
class GsonMqttObjectMapper(private val gson: Gson) : MqttObjectMapper {
    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any =
        gson.fromJson(bytes.decodeToString(), targetType)

    override fun toBytes(value: Any): String = gson.toJson(value)
}
