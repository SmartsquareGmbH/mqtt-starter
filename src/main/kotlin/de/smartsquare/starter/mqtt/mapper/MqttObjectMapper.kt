package de.smartsquare.starter.mqtt.mapper

/**
 * Interface for object serialization/deserialization abstraction.
 * Allows support for different libraries (Jackson, Gson, etc.)
 */
interface MqttObjectMapper {

    /**
     * Deserializes the given [bytes] into an object of the specified [targetType].
     */
    fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any

    /**
     * Serializes the given [value] into a string.
     */
    fun toBytes(value: Any): String
}
