package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientBuilder
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executor

/**
 * Main entry point for the spring autoconfiguration. Exposes all necessary beans for connection,
 * subscription and publishing to configured mqtt broker.
 */
@Suppress("TooManyFunctions")
@Configuration
@ConditionalOnClass(MqttClient::class)
@ConditionalOnProperty("mqtt.enabled", matchIfMissing = true)
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
        mqttExecutor: Executor,
        configurers: List<Mqtt3ClientConfigurer>,
    ): Mqtt3Client {
        val clientBuilder = configureCommon(config, mqttExecutor)
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
        mqttExecutor: Executor,
        configurers: List<Mqtt5ClientConfigurer>,
    ): Mqtt5Client {
        val clientBuilder = configureCommon(config, mqttExecutor)
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

    private fun configureCommon(config: MqttProperties, executor: Executor): MqttClientBuilder {
        return MqttClient.builder()
            .serverHost(config.host)
            .serverPort(config.port)
            .automaticReconnectWithDefaultConfig()
            .executorConfig()
            .applicationScheduler(Schedulers.from(executor))
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
    fun mqttExecutor(): Executor = MqttExecutor()

    @Bean
    fun annotationCollector() = MqttAnnotationCollector()

    @Bean
    fun messageAdapter(): MqttMessageAdapter {
        return MqttMessageAdapter(jacksonObjectMapper().findAndRegisterModules())
    }

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
    fun mqtt3Connector(config: MqttProperties, client: Mqtt3Client): Mqtt3Connector {
        return Mqtt3Connector(client, config)
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Connector(config: MqttProperties, client: Mqtt5Client): Mqtt5Connector {
        return Mqtt5Connector(client, config)
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "3", matchIfMissing = true)
    fun mqtt3Router(
        messageAdapter: MqttMessageAdapter,
        collector: MqttAnnotationCollector,
        messageErrorHandler: MqttMessageErrorHandler,
        config: MqttProperties,
        client: Mqtt3Client,
    ): Mqtt3Router {
        return Mqtt3Router(collector, messageAdapter, messageErrorHandler, config, client)
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Router(
        messageAdapter: MqttMessageAdapter,
        collector: MqttAnnotationCollector,
        messageErrorHandler: MqttMessageErrorHandler,
        config: MqttProperties,
        client: Mqtt5Client,
    ): Mqtt5Router {
        return Mqtt5Router(collector, messageAdapter, messageErrorHandler, config, client)
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
