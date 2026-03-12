package de.smartsquare.starter.mqtt.health

import com.hivemq.client.mqtt.MqttClient
import de.smartsquare.starter.mqtt.MqttAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator
import org.springframework.context.annotation.Bean

/**
 * Autoconfiguration for the mqtt health indicator.
 */
@ConditionalOnBean(MqttClient::class)
@ConditionalOnClass(HealthEndpoint::class)
@ConditionalOnEnabledHealthIndicator("mqtt")
@AutoConfiguration(after = [MqttAutoConfiguration::class])
class MqttHealthAutoConfiguration {
    @Bean
    fun mqttHealthIndicator(client: MqttClient) = MqttHealthIndicator(client)
}
