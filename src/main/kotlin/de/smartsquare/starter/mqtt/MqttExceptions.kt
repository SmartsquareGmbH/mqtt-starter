package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttTopic

/**
 * Exception thrown when invalid methods with [MqttSubscribe] are found.
 */
class MqttConfigurationException(message: String) : RuntimeException(message)

/**
 * Exception thrown when the connection to mqtt broker fails.
 */
class MqttBrokerConnectException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Exception thrown when processing a single mqtt message fails.
 */
class MqttMessageException(
    val topic: MqttTopic,
    val payload: ByteArray,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
