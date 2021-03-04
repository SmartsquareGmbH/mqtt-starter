package de.smartsquare.smartbot.starter.mqtt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotEmpty

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "mqtt")
class MqttProperties(

    val port: Int,

    @NotEmpty
    val host: String,

    @NotEmpty
    val clientId: String,

    @NotEmpty
    val username: String,

    @NotEmpty
    val password: String,

    val ssl: Boolean
)
