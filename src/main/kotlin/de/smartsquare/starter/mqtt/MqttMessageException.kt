package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttTopic

class MqttMessageException(
    val topic: MqttTopic,
    val payload: ByteArray,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
