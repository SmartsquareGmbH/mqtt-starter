package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.MqttClient
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

@ConditionalOnBean(MqttClient::class)
@ConditionalOnClass(HealthEndpoint::class)
@ConditionalOnEnabledHealthIndicator("mqtt")
@AutoConfiguration(after = [MqttAutoConfiguration::class])
class MqttHealthAutoConfiguration {
    @Bean
    fun mqttHealthIndicator(client: MqttClient) = MqttHealthIndicator(client)
}
