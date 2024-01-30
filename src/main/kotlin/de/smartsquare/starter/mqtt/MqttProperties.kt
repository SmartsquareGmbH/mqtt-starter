package de.smartsquare.starter.mqtt

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Properties for connection to the mqtt broker.
 */
@Validated
@ConfigurationProperties(prefix = "mqtt")
data class MqttProperties(

    /**
     * The host the mqtt broker is available under.
     */
    @get:NotEmpty
    val host: String = "localhost",

    /**
     * The port the mqtt broker is available under.
     */
    @field:Min(0)
    @field:Max(65535)
    val port: Int = 1883,

    /**
     * The client id this component should connect with.
     */
    val clientId: String? = null,

    /**
     * The username this component should connect with.
     */
    val username: String? = null,

    /**
     * The password this component should connect with.
     */
    val password: String? = null,

    /**
     * If ssl should be used for the connection to the mqtt broker.
     */
    val ssl: Boolean = false,

    /**
     * If the client should connect with a clean session.
     */
    val clean: Boolean = true,

    /**
     * The optional group subscriptions should be prefixed with.
     */
    val group: String? = null,

    /**
     * The mqtt protocol version to use.
     */
    @get:MqttVersion
    val version: Int = 3,

    /**
     * The timeout for connection to the broker in milliseconds.
     */
    val connectTimeout: Long = 10_000,
)
