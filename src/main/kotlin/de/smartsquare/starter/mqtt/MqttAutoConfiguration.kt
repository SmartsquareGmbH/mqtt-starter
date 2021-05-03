package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
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

@Configuration
@ConditionalOnClass(MqttClient::class)
@EnableConfigurationProperties(MqttProperties::class)
class MqttAutoConfiguration {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun mqttClient(config: MqttProperties): Mqtt3Client {
        val baseClient = Mqtt3Client.builder()
            .serverHost(config.host)
            .serverPort(config.port)
            .automaticReconnectWithDefaultConfig()
            .simpleAuth()
            .username(config.username)
            .password(config.password.toByteArray())
            .applySimpleAuth()

        val builder = if (config.ssl) {
            baseClient.sslWithDefaultConfig()
        } else {
            baseClient
        }

        val client = if (config.clientId != null) {
            builder.identifier(config.clientId!!).build()
        } else {
            builder.build()
        }

        logger.info("Connecting to ${config.username}@${config.host}:${config.port}...")

        try {
            val acknowledgement = client.toAsync().connect().get(10, SECONDS)

            if (acknowledgement.returnCode.isError) {
                throw BrokerConnectException(acknowledgement)
            } else {
                logger.info("Successfully connected to broker.")

                return client
            }
        } catch (e: TimeoutException) {
            throw BrokerConnectException("Broker ${config.host}:${config.port} did not respond within 10 seconds.", e)
        }
    }

    @Bean
    fun annotationCollector() = AnnotationCollector()

    @Bean
    fun jackson(): ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    @Bean
    fun messageAdapter(objectMapper: ObjectMapper): MqttMessageAdapter {
        return MqttMessageAdapter(objectMapper)
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
