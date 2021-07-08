package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Main entry point for the spring auto configuration. Exposes all necessary beans for connection,
 * subscription and publishing to configured mqtt broker.
 */
@Configuration
@ConditionalOnClass(MqttClient::class)
@EnableConfigurationProperties(MqttProperties::class)
class MqttAutoConfiguration {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Returns a configured and ready to use mqtt client.
     */
    @Bean
    fun mqttClient(config: MqttProperties, configurers: List<MqttClientConfigurer>): Mqtt3Client {
        val clientBuilder = Mqtt3Client.builder()
            .serverHost(config.host)
            .serverPort(config.port)
            .automaticReconnectWithDefaultConfig()
            .simpleAuth()
            .username(config.username)
            .password(config.password.toByteArray())
            .applySimpleAuth()
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
            .apply { configurers.forEach { configurer -> configurer.configure(this) } }

        val connectOptions = Mqtt3Connect.builder()
            .cleanSession(config.clean)
            .build()

        return SpringAwareMqtt3Client(clientBuilder.build(), connectOptions)
    }

    @Bean
    fun annotationCollector() = AnnotationCollector()

    @Bean
    fun messageAdapter(): MqttMessageAdapter {
        return MqttMessageAdapter(jacksonObjectMapper().findAndRegisterModules())
    }

    @Bean
    fun router(
        messageAdapter: MqttMessageAdapter,
        collector: AnnotationCollector,
        config: MqttProperties,
        client: Mqtt3Client
    ): MqttRouter {
        return MqttRouter(collector, messageAdapter, config, client)
    }

    @Bean
    fun publisher(
        messageAdapter: MqttMessageAdapter,
        client: Mqtt3Client
    ): MqttPublisher {
        return MqttPublisher(messageAdapter, client)
    }
}
