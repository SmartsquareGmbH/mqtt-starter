package de.smartsquare.smartbot.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

@Configuration
@ConditionalOnClass(MqttClient::class)
@EnableConfigurationProperties(MqttProperties::class)
open class MqttAutoConfiguration {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    open fun mqttClient(config: MqttProperties): Mqtt3Client {
        val baseClient = Mqtt3Client.builder()
            .identifier(config.clientId)
            .serverHost(config.host)
            .serverPort(config.port)
            .automaticReconnect()
            .initialDelay(1, SECONDS)
            .maxDelay(1, MINUTES)
            .applyAutomaticReconnect()
            .simpleAuth()
            .username(config.username)
            .password(config.password.toByteArray())
            .applySimpleAuth()

        val client = if (config.ssl) {
            baseClient.sslWithDefaultConfig().build()
        } else {
            baseClient.build()
        }

        logger.debug("Configured mqtt client. (${config.username}@${config.host}:${config.port})")
        client.toBlocking().connect()
        logger.debug("Connected to broker.")

        return client
    }

    @Bean
    open fun annotationCollector() = AnnotationCollector()

    @Bean
    open fun jackson(): ObjectMapper = jacksonObjectMapper().findAndRegisterModules()

    @Bean
    open fun adapter(collector: AnnotationCollector, mapper: ObjectMapper, mqttClient: Mqtt3Client): Adapter {
        return Adapter(collector, mapper, mqttClient)
    }
}
