package de.smartsquare.starter.mqtt.health

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientState.CONNECTED
import com.hivemq.client.mqtt.MqttClientState.CONNECTING
import com.hivemq.client.mqtt.MqttClientState.CONNECTING_RECONNECT
import com.hivemq.client.mqtt.MqttClientState.DISCONNECTED
import com.hivemq.client.mqtt.MqttClientState.DISCONNECTED_RECONNECT
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator

/**
 * Health indicator for the mqtt client based on its connection state.
 */
class MqttHealthIndicator(private val client: MqttClient) : HealthIndicator {
    override fun health(): Health = when (val state = client.state) {
        CONNECTED -> Health.up().build()
        DISCONNECTED, CONNECTING, DISCONNECTED_RECONNECT, CONNECTING_RECONNECT ->
            Health.down().withDetail("state", state).build()
    }
}
