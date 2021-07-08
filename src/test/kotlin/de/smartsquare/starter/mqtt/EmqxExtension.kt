package de.smartsquare.starter.mqtt

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EmqxExtension : BeforeAllCallback {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private val logConsumer get() = Slf4jLogConsumer(logger).withSeparateOutputStreams()

        private val emqxImageName = DockerImageName.parse("emqx/emqx:4.3.1")

        private val emqx = KGenericContainer(emqxImageName)
            .withEnv("EMQX_LOADED_PLUGINS", "emqx_auth_mnesia")
            .withEnv("EMQX_AUTH__USER__1__USERNAME", "admin")
            .withEnv("EMQX_AUTH__USER__1__PASSWORD", "public")
            .withEnv("EMQX_ALLOW_ANONYMOUS", "false")
            .withEnv("WAIT_FOR_ERLANG", "60")
            .withExposedPorts(1883, 8081, 18083)
            .waitingFor(Wait.forLogMessage(".*is running now!.*", 1))
            .withLogConsumer(logConsumer.withPrefix("emqx"))
    }

    private val lock = ReentrantLock()

    override fun beforeAll(context: ExtensionContext) {
        val store = context.root.getStore(ExtensionContext.Namespace.GLOBAL)

        lock.withLock {
            val emqxStarted = store.getOrDefault("emqx", Boolean::class.java, false)

            if (!emqxStarted) {
                emqx.start()

                store.put("emqx", true)
            }
        }

        System.setProperty("mqtt.host", emqx.host)
        System.setProperty("mqtt.port", emqx.getMappedPort(1883).toString())
        System.setProperty("mqtt.username", "admin")
        System.setProperty("mqtt.password", "public")
        System.setProperty("mqtt.ssl", "false")
    }
}
