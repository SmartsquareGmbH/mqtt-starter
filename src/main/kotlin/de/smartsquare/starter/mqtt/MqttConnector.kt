package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import java.util.concurrent.TimeUnit

abstract class MqttConnector : SmartLifecycle {

    companion object {
        const val SMART_LIFECYCLE_PHASE = SmartLifecycle.DEFAULT_PHASE - 1024
    }

    override fun stop() {
        throw UnsupportedOperationException("Stop must not be invoked directly")
    }

    override fun getPhase(): Int {
        return SMART_LIFECYCLE_PHASE
    }

    abstract override fun stop(callback: Runnable)
}

class Mqtt3Connector(
    client: Mqtt3Client,
    private val config: MqttProperties,
) : MqttConnector() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val client = client.toAsync()

    override fun start() {
        val host = client.config.serverHost
        val port = client.config.serverPort
        val username = client.config.simpleAuth.orElseGet { null }?.username?.toString()

        val connectOptions = Mqtt3Connect.builder()
            .cleanSession(config.clean)
            .build()

        logger.info("Connecting to ${if (username != null) "$username@" else ""}$host:$port using mqtt 3...")

        try {
            client.connect(connectOptions).get(config.connectTimeout, TimeUnit.MILLISECONDS)
        } catch (error: Exception) {
            throw MqttBrokerConnectException("Failed to connect to $host:$port", error)
        }
    }

    override fun stop(callback: Runnable) {
        client.disconnect().thenRun(callback)
    }

    override fun isRunning() = client.state != MqttClientState.DISCONNECTED
}

class Mqtt5Connector(
    client: Mqtt5Client,
    private val config: MqttProperties,
) : MqttConnector() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val client = client.toAsync()

    override fun start() {
        val host = client.config.serverHost
        val port = client.config.serverPort
        val username = client.config.simpleAuth.flatMap { it.username }.orElseGet { null }?.toString()

        val connectOptions = Mqtt5Connect.builder()
            .cleanStart(config.clean)
            .build()

        logger.info("Connecting to ${if (username != null) "$username@" else ""}$host:$port using mqtt 5...")

        try {
            client.connect(connectOptions).get(config.connectTimeout, TimeUnit.MILLISECONDS)
        } catch (error: Exception) {
            throw MqttBrokerConnectException("Failed to connect to $host:$port", error)
        }
    }

    override fun stop(callback: Runnable) {
        client.disconnect().thenRun(callback)
    }

    override fun isRunning() = client.state != MqttClientState.DISCONNECTED
}
