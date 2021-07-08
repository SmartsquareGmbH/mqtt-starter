package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder

interface MqttClientConfigurer {
    fun configure(builder: Mqtt3ClientBuilder)
}
