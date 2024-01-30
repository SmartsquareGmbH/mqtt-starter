package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.MqttClientState
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Base class for connectors implementing common logic.
 */
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

/**
 * Class responsible for connecting a client (mqtt 3) and subscribing to collected topics.
 */
class Mqtt3Connector(
    client: Mqtt3Client,
    private val collector: MqttSubscriberCollector,
    private val handler: MqttHandler,
    private val config: MqttProperties,
) : MqttConnector() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val client = client.toAsync()

    override fun start() {
        try {
            CompletableFuture.allOf(subscribe(), connect()).get(config.connectTimeout, TimeUnit.MILLISECONDS)
        } catch (error: TimeoutException) {
            throw MqttBrokerConnectException("Timeout while connecting to broker", error)
        }
    }

    private fun subscribe(): CompletableFuture<Mqtt3SubAck> {
        val subscriptions = collector.subscribers.map {
            Mqtt3Subscription.builder()
                .topicFilter(it.topic)
                .qos(it.qos)
                .build()
        }

        if (subscriptions.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        return client.subscribeWith()
            .addSubscriptions(subscriptions)
            .callback { handler.handle(it.topic, it.payloadAsBytes) }
            .send()
            .exceptionallyCompose {
                CompletableFuture.failedFuture(MqttBrokerConnectException("Failed to subscribe", it))
            }
    }

    private fun connect(): CompletableFuture<Mqtt3ConnAck> {
        val host = client.config.serverHost
        val port = client.config.serverPort
        val username = client.config.simpleAuth.orElseGet { null }?.username?.toString()

        val connectOptions = Mqtt3Connect.builder()
            .cleanSession(config.clean)
            .build()

        logger.info("Connecting to ${if (username != null) "$username@" else ""}$host:$port using mqtt 3...")

        return client.connect(connectOptions)
            .exceptionallyCompose {
                CompletableFuture.failedFuture(
                    MqttBrokerConnectException("Failed to connect to broker $host:$port", it),
                )
            }
    }

    override fun stop(callback: Runnable) {
        client.disconnect().thenRun(callback)
    }

    override fun isRunning() = client.state != MqttClientState.DISCONNECTED
}

/**
 * Class responsible for connecting a client (mqtt 5) and subscribing to collected topics.
 */
class Mqtt5Connector(
    client: Mqtt5Client,
    private val collector: MqttSubscriberCollector,
    private val handler: MqttHandler,
    private val config: MqttProperties,
) : MqttConnector() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val client = client.toAsync()

    override fun start() {
        try {
            CompletableFuture.allOf(subscribe(), connect()).get(config.connectTimeout, TimeUnit.MILLISECONDS)
        } catch (error: TimeoutException) {
            throw MqttBrokerConnectException("Timeout while connecting to broker", error)
        }
    }

    private fun subscribe(): CompletableFuture<Mqtt5SubAck> {
        val subscriptions = collector.subscribers.map {
            Mqtt5Subscription.builder()
                .topicFilter(it.topic)
                .qos(it.qos)
                .build()
        }

        if (subscriptions.isEmpty()) {
            return CompletableFuture.completedFuture(null)
        }

        return client.subscribeWith()
            .addSubscriptions(subscriptions)
            .callback { handler.handle(it.topic, it.payloadAsBytes) }
            .send()
            .exceptionallyCompose {
                CompletableFuture.failedFuture(MqttBrokerConnectException("Failed to subscribe", it))
            }
    }

    private fun connect(): CompletableFuture<Mqtt5ConnAck> {
        val host = client.config.serverHost
        val port = client.config.serverPort
        val username = client.config.simpleAuth.flatMap { it.username }.orElseGet { null }?.toString()

        val connectOptions = Mqtt5Connect.builder()
            .cleanStart(config.clean)
            .build()

        logger.info("Connecting to ${if (username != null) "$username@" else ""}$host:$port using mqtt 3...")

        return client.connect(connectOptions)
            .exceptionallyCompose {
                CompletableFuture.failedFuture(
                    MqttBrokerConnectException("Failed to connect to broker $host:$port", it),
                )
            }
    }

    override fun stop(callback: Runnable) {
        client.disconnect().thenRun(callback)
    }

    override fun isRunning() = client.state != MqttClientState.DISCONNECTED
}