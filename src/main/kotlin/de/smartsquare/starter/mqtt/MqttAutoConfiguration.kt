package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientBuilder
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Main entry point for the spring auto configuration. Exposes all necessary beans for connection,
 * subscription and publishing to configured mqtt broker.
 */
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
    fun mqtt3Client(config: MqttProperties, configurers: List<Mqtt3ClientConfigurer>): Mqtt3Client {
        val clientBuilder = configureCommon(config)
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

        val connectOptions = Mqtt3Connect.builder()
            .cleanSession(config.clean)
            .build()

        return SpringAwareMqtt3Client(clientBuilder.build(), connectOptions)
    }

    /**
     * Returns a configured and ready to use mqtt 5 client.
     */
    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Client(config: MqttProperties, configurers: List<Mqtt5ClientConfigurer>): Mqtt5Client {
        val clientBuilder = configureCommon(config)
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

        val connectOptions = Mqtt5Connect.builder()
            .cleanStart(config.clean)
            .build()

        return SpringAwareMqtt5Client(clientBuilder.build(), connectOptions)
    }

    private fun configureCommon(config: MqttProperties): MqttClientBuilder {
        return MqttClient.builder()
            .serverHost(config.host)
            .serverPort(config.port)
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener { logger.info("Connected to broker.") }
            .addDisconnectedListener {
                if (it.reconnector.isReconnect) {
                    logger.warn("Disconnected from broker, reconnecting...")
                } else {
                    logger.info("Disconnected from broker.")
                }
            }
            .apply { if (config.ssl) sslWithDefaultConfig() }
            .apply { config.clientId?.also { clientId -> identifier(clientId) } }
    }

    @Bean
    fun annotationCollector() = AnnotationCollector()

    @Bean
    fun messageAdapter(): MqttMessageAdapter {
        return MqttMessageAdapter(jacksonObjectMapper().findAndRegisterModules())
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "3", matchIfMissing = true)
    fun mqtt3Router(
        messageAdapter: MqttMessageAdapter,
        collector: AnnotationCollector,
        config: MqttProperties,
        client: Mqtt3Client
    ): Mqtt3Router {
        return Mqtt3Router(collector, messageAdapter, config, client)
    }

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Router(
        messageAdapter: MqttMessageAdapter,
        collector: AnnotationCollector,
        config: MqttProperties,
        client: Mqtt5Client
    ): Mqtt5Router {
        return Mqtt5Router(collector, messageAdapter, config, client)
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
