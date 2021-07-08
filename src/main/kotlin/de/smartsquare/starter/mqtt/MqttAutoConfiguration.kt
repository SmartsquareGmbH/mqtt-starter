package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException

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
     * Returns a configured and connected mqtt client.
     */
    @Bean
    fun mqttClient(config: MqttProperties): Mqtt3Client {
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
            .apply { if(config.ssl) sslWithDefaultConfig() }
            .apply { config.clientId?.also { clientId -> identifier(clientId) } }

        val mqttClient = clientBuilder.build()

        logger.info("Connecting to ${config.username}@${config.host}:${config.port}...")

        try {
            val acknowledgement = mqttClient.toAsync()
                .connectWith()
                .cleanSession(config.clean)
                .send().get(10, SECONDS)

            if (acknowledgement.returnCode.isError) {
                throw BrokerConnectException(acknowledgement)
            } else {
                return DisposableMqtt3Client(mqttClient)
            }
        } catch (e: TimeoutException) {
            throw BrokerConnectException("Broker ${config.host}:${config.port} did not respond within 10 seconds.", e)
        }
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
