package de.smartsquare.starter.mqtt.mapper

import com.google.gson.Gson
import org.springframework.beans.factory.ObjectProvider

/**
 * Gson implementation of [MqttObjectMapper].
 */
class GsonMqttObjectMapper(provider: ObjectProvider<Gson>) : MqttObjectMapper {
    private val gson by lazy { provider.getObject() }

    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any =
        gson.fromJson(bytes.decodeToString(), targetType)

    override fun toBytes(value: Any): String = gson.toJson(value)
}
