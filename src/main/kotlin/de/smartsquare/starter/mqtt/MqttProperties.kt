package de.smartsquare.starter.mqtt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty

/**
 * Properties for connection to the mqtt broker.
 */
@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "mqtt")
data class MqttProperties(

    /**
     * The port the mqtt broker is available under.
     */
    val port: Int,

    /**
     * The host the mqtt broker is available under.
     */
    @NotEmpty
    val host: String,

    /**
     * The client id this component should connect with.
     */
    @NotEmpty
    val clientId: String? = null,

    /**
     * The username this component should connect with.
     */
    @NotEmpty
    val username: String,

    /**
     * The password this component should connect with.
     */
    @NotEmpty
    val password: String,

    /**
     * If ssl should be used for the connection to the mqtt broker.
     */
    val ssl: Boolean,

    /**
     * If the client should connect with a clean session.
     */
    val clean: Boolean = true,

    /**
     * The optional group subscriptions should be prefixed with.
     *
     * See [shared subscriptions](https://www.hivemq.com/blog/mqtt5-essentials-part7-shared-subscriptions).
     */
    val group: String? = null
)
