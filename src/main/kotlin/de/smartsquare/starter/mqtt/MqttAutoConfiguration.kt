package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import de.smartsquare.starter.mqtt.mapper.ErrorMqttObjectMapper
import de.smartsquare.starter.mqtt.mapper.GsonMqttObjectMapper
import de.smartsquare.starter.mqtt.mapper.Jackson2MqttObjectMapper
import de.smartsquare.starter.mqtt.mapper.JacksonMqttObjectMapper
import de.smartsquare.starter.mqtt.mapper.MqttObjectMapper
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.Executor

/**
 * Main entry point for the spring autoconfiguration. Exposes all necessary beans for connection,
 * subscription and publishing to configured mqtt broker.
 */
@Suppress("TooManyFunctions")
@AutoConfiguration
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
    fun mqtt3Client(config: MqttProperties, mqttScheduler: Scheduler, configurers: List<Mqtt3ClientConfigurer>) =
        configureCommon(config, mqttScheduler)
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
            .build()

    /**
     * Returns a configured and ready to use mqtt 5 client.
     */
    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Client(config: MqttProperties, mqttScheduler: Scheduler, configurers: List<Mqtt5ClientConfigurer>) =
        configureCommon(config, mqttScheduler)
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
            .build()

    private fun configureCommon(config: MqttProperties, scheduler: Scheduler) = MqttClient.builder()
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
        .let { if (config.ssl) it.sslWithDefaultConfig() else it }
        .let { config.clientId?.let { clientId -> it.identifier(clientId) } ?: it }

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
    @Order(1)
    @ConditionalOnMissingBean
    @ConditionalOnBean(JsonMapper::class)
    fun jacksonMqttObjectMapper(provider: ObjectProvider<JsonMapper>): MqttObjectMapper =
        JacksonMqttObjectMapper(provider.getObject())

    @Bean
    @Order(2)
    @ConditionalOnMissingBean
    @ConditionalOnBean(ObjectMapper::class)
    fun jackson2MqttObjectMapper(provider: ObjectProvider<ObjectMapper>): MqttObjectMapper =
        Jackson2MqttObjectMapper(provider.getObject())

    @Bean
    @Order(3)
    @ConditionalOnMissingBean
    @ConditionalOnBean(Gson::class)
    fun gsonMqttObjectMapper(provider: ObjectProvider<Gson>): MqttObjectMapper =
        GsonMqttObjectMapper(provider.getObject())

    @Bean
    @ConditionalOnMissingBean
    fun fallbackMqttObjectMapper(): MqttObjectMapper = ErrorMqttObjectMapper()

    @Bean
    @ConditionalOnMissingBean
    fun mqttSubscriberRegistry(beanFactory: ListableBeanFactory, config: MqttProperties) =
        MqttSubscriberRegistry(beanFactory, config)

    @Bean
    @ConditionalOnMissingBean
    fun mqttMessageAdapter(mqttObjectMapper: MqttObjectMapper) = MqttMessageAdapter(mqttObjectMapper)

    @Bean
    fun mqttHandler(
        registry: MqttSubscriberRegistry,
        adapter: MqttMessageAdapter,
        messageErrorHandler: MqttMessageErrorHandler,
    ) = MqttHandler(registry, adapter, messageErrorHandler)

    @Bean
    @ConditionalOnMissingBean
    fun mqttMessageErrorHandler() = MqttMessageErrorHandler()

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("mqtt.version", havingValue = "3", matchIfMissing = true)
    fun mqtt3Connector(
        client: Mqtt3Client,
        registry: MqttSubscriberRegistry,
        handler: MqttHandler,
        config: MqttProperties,
    ): MqttConnector = Mqtt3Connector(client, registry, handler, config)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Connector(
        client: Mqtt5Client,
        registry: MqttSubscriberRegistry,
        handler: MqttHandler,
        config: MqttProperties,
    ): MqttConnector = Mqtt5Connector(client, registry, handler, config)

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "3", matchIfMissing = true)
    fun mqtt3Publisher(messageAdapter: MqttMessageAdapter, client: Mqtt3Client) = Mqtt3Publisher(messageAdapter, client)

    @Bean
    @ConditionalOnProperty("mqtt.version", havingValue = "5")
    fun mqtt5Publisher(messageAdapter: MqttMessageAdapter, client: Mqtt5Client) = Mqtt5Publisher(messageAdapter, client)
}
