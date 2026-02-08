package de.smartsquare.starter.mqtt

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class EmqxExtension : BeforeAllCallback {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private val logConsumer get() = Slf4jLogConsumer(logger).withSeparateOutputStreams()

        private val emqx = GenericContainer(DockerImageName.parse("emqx/emqx:6.1.0"))
            .withExposedPorts(1883, 18083)
            .withEnv("EMQX_NODE__COOKIE", "Y8PKwtA3HLks1EEX")
            .withEnv("EMQX_MQTT__STRICT_MODE", "true")
            .withEnv("EMQX_ALLOW_ANONYMOUS", "true")
            .waitingFor(Wait.forHttp("/status").forPort(18083))
            .withLogConsumer(logConsumer.withPrefix("emqx"))
    }

    override fun beforeAll(context: ExtensionContext) {
        emqx.start()

        System.setProperty("mqtt.host", emqx.host)
        System.setProperty("mqtt.port", emqx.getMappedPort(1883).toString())
        System.setProperty("mqtt.username", "admin")
        System.setProperty("mqtt.password", "public")
    }
}
