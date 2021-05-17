package de.smartsquare.starter.mqtt

/**
 * Exception thrown when invalid methods with [MqttSubscribe] are found.
 */
class MqttConfigurationException(message: String) : RuntimeException(message)
