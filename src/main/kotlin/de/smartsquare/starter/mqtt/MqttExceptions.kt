package de.smartsquare.starter.mqtt

/**
 * Exception thrown when invalid methods with [MqttSubscribe] are found.
 */
class MqttConfigurationException(message: String) : RuntimeException(message)

/**
 * Exception thrown when the connection to mqtt broker fails.
 */
class MqttBrokerConnectException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
