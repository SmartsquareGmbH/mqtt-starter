package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientBuilder
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.util.concurrent.Executor

/**
 * Main entry point for the spring autoconfiguration. Exposes all necessary beans for connection,
 * subscription and publishing to configured mqtt broker.
 */
@Suppress("TooManyFunctions")
@AutoConfiguration
@AutoConfigureAfter(JacksonAutoConfiguration::class)
@Import(MqttSubscriberCollector::class)
@ConditionalOnClass(MqttClient::class)
@ConditionalOnProperty("mqtt.enabled", matchIfMissing = true)
@RegisterReflectionForBinding(MqttProperties::class)
@EnableConfigurationProperties(MqttProperties::class)
class MqttAutoConfiguration {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Returns a configured and ready to use mqtt 3 client.
     */
    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "3", matchIfMissing = true)
    fun mqtt3Client(
        config: MqttProperties,
        mqttScheduler: Scheduler,
        configurers: List<Mqtt3ClientConfigurer>,
    ): Mqtt3Client {
        val clientBuilder = configureCommon(config, mqttScheduler)
            .useMqttVersion3()
            .apply {
                config.username?.let { username ->
                    config.password?.let { password ->
                        simpleAuth()
                            .username(username)
                            .password(password.toByteArray())
                            .applySimpleAuth()
                    }
                }
            }
            .apply { configurers.forEach { configurer -> configurer.configure(this) } }

        return clientBuilder.build()
    }

    /**
     * Returns a configured and ready to use mqtt 5 client.
     */
    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Client(
        config: MqttProperties,
        mqttScheduler: Scheduler,
        configurers: List<Mqtt5ClientConfigurer>,
    ): Mqtt5Client {
        val clientBuilder = configureCommon(config, mqttScheduler)
            .useMqttVersion5()
            .apply {
                config.username?.let { username ->
                    config.password?.let { password ->
                        simpleAuth()
                            .username(username)
                            .password(password.toByteArray())
                            .applySimpleAuth()
                    }
                }
            }
            .apply { configurers.forEach { configurer -> configurer.configure(this) } }

        return clientBuilder.build()
    }

    private fun configureCommon(config: MqttProperties, scheduler: Scheduler): MqttClientBuilder {
        return MqttClient.builder()
            .serverHost(config.host)
            .serverPort(config.port)
            .automaticReconnectWithDefaultConfig()
            .executorConfig()
            .applicationScheduler(scheduler)
            .applyExecutorConfig()
            .addConnectedListener { logger.info("Connected to broker.") }
            .addDisconnectedListener {
                if (it.reconnector.isReconnect) {
                    if (logger.isDebugEnabled) {
                        logger.warn("Disconnected from broker, reconnecting...", it.cause)
                    } else {
                        logger.warn("Disconnected from broker, reconnecting...")
                    }
                } else {
                    logger.info("Disconnected from broker.")
                }
            }
            .apply { if (config.ssl) sslWithDefaultConfig() }
            .apply { config.clientId?.also { clientId -> identifier(clientId) } }
    }

    @Bean
    @ConditionalOnProperty("mqtt.shutdown", havingValue = "graceful", matchIfMissing = true)
    fun mqttExecutor(): Executor = MqttGracefulExecutor()

    @Bean
    @ConditionalOnProperty("mqtt.shutdown", havingValue = "graceful", matchIfMissing = true)
    fun gracefulMqttScheduler(mqttExecutor: Executor): Scheduler = Schedulers.from(mqttExecutor)

    @Bean
    @ConditionalOnProperty("mqtt.shutdown", havingValue = "immediate")
    fun immediateMqttScheduler(): Scheduler = Schedulers.computation()

    @Bean
    @ConditionalOnMissingBean
    fun mqttMessageAdapter(objectMapper: ObjectMapper): MqttMessageAdapter = DefaultMqttMessageAdapter(objectMapper)

    /**
     * Configures a basic [ObjectMapper] if none is available already.
     */
    @Bean
    @ConditionalOnMissingBean
    fun fallbackObjectMapper(): ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    @Bean
    fun mqttHandler(
        collector: MqttSubscriberCollector,
        adapter: MqttMessageAdapter,
        messageErrorHandler: MqttMessageErrorHandler,
    ): MqttHandler = MqttHandler(collector, adapter, messageErrorHandler)

    /**
     * Returns a default mqtt message error handler.
     */
    @Bean
    @ConditionalOnMissingBean
    fun mqttMessageErrorHandler(): MqttMessageErrorHandler {
        return MqttMessageErrorHandler()
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "3", matchIfMissing = true)
    fun mqtt3Connector(
        client: Mqtt3Client,
        collector: MqttSubscriberCollector,
        handler: MqttHandler,
        config: MqttProperties,
    ): Mqtt3Connector {
        return Mqtt3Connector(client, collector, handler, config)
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Connector(
        client: Mqtt5Client,
        collector: MqttSubscriberCollector,
        handler: MqttHandler,
        config: MqttProperties,
    ): Mqtt5Connector {
        return Mqtt5Connector(client, collector, handler, config)
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "3", matchIfMissing = true)
    fun mqtt3Publisher(messageAdapter: MqttMessageAdapter, client: Mqtt3Client): Mqtt3Publisher {
        return Mqtt3Publisher(messageAdapter, client)
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Publisher(messageAdapter: MqttMessageAdapter, client: Mqtt5Client): Mqtt5Publisher {
        return Mqtt5Publisher(messageAdapter, client)
    }
}
