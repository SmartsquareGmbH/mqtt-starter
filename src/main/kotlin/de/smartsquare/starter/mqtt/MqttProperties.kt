package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
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
    val host: String = "",

    /**
     * The port the mqtt broker is available under.
     */
    @get:Min(1)
    @get:Max(65535)
    val port: Int = 0,

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

    /**
     * The shutdown configuration for the mqtt processor.
     */
    val shutdown: MqttShutdown = MqttShutdown.GRACEFUL,

    /**
     * The session expiry interval in seconds. Has to be in [0, 4294967295] (0 by default).
     * Setting the value to 0 means the session will expire immediately after disconnect.
     * Setting it to 4_294_967_295 means the session will never expire.
     * This setting is only going into effect for MQTT 5.
     */
    @get:Min(0)
    @get:Max(Mqtt5Connect.NO_SESSION_EXPIRY)
    val sessionExpiry: Long = Mqtt5Connect.DEFAULT_SESSION_EXPIRY_INTERVAL,
) {

    /**
     * Configuration for shutting a mqtt processor.
     */
    enum class MqttShutdown {

        /**
         * The mqtt processor should support graceful shutdown, allowing active tasks time to complete.
         */
        GRACEFUL,

        /**
         * The mqtt processor should shut down immediately.
         */
        IMMEDIATE,
    }
}
