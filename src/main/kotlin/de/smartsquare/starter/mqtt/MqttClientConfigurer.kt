package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder

/**
 * Interface to enable more advanced configuration for the [Mqtt3ClientBuilder] than what is possible with the
 * properties.
 */
fun interface Mqtt3ClientConfigurer {

    /**
     * To be implemented by consumers. Can perform any configuration on the given [builder] in place.
     * This is called after all other configuration have been done so this is the final step.
     */
    fun configure(builder: Mqtt3ClientBuilder)
}

/**
 * Interface to enable more advanced configuration for the [Mqtt5ClientBuilder] than what is possible with the
 * properties.
 */
fun interface Mqtt5ClientConfigurer {

    /**
     * To be implemented by consumers. Can perform any configuration on the given [builder] in place.
     * This is called after all other configuration have been done so this is the final step.
     */
    fun configure(builder: Mqtt5ClientBuilder)
}
