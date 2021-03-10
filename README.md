# :honeybee: HiveMQ Spring Boot Starter

This project contains a basic configuration to consume mqtt messages using the hivemq client. 

## Getting Started

### Gradle Configuration

```groovy
repositories {
	maven("https://maven.pkg.github.com/SmartsquareGmbH/mqtt-starter") {
		credentials {
			username = System.properties['GH_USER']
			password = System.properties['GH_TOKEN'] // The token must have at least the privilege to read packages.
		}
	}
}

dependencies {
	implementation("de.smartsquare:mqtt-starter:0.9.0")
	implementation("com.hivemq:hivemq-mqtt-client:1.2.1")
}
```

### Application Properties

```properties
mqtt.host: test.mosquitto.org
mqtt.port: 1883

mqtt.client-id: test
mqtt.username: admin
mqtt.password: test

mqtt.ssl: false
```

### Consumer Endpoints

```kotlin
import com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE
import com.hivemq.client.mqtt.datatypes.MqttTopic
import de.smartsquare.starter.mqtt.MqttSubscribe
import org.springframework.stereotype.Component

@Component
class TestConsumer {

    // ✅ Topic and payload
    @MqttSubscribe(topic = "/home/+/temperature", qos = AT_LEAST_ONCE)
    fun subscribe(payload: TemperaturePayload, topic: MqttTopic) {
        println("Temperature is ${payload.value} °C in room ${topic.levels[1]}]")
    }

    class TemperaturePayload(val value: Int)

    // ✅ Only topic
    @MqttSubscribe(topic = "/home/+/light/on", qos = AT_LEAST_ONCE)
    fun subscribe(topic: MqttTopic) {
        println("Light turned on in room ${topic.levels[1]}]")
    }

    // ✅ Only numeric payload
    @MqttSubscribe(topic = "/home/ping", qos = AT_LEAST_ONCE)
    fun subscribe(ping: Int) {
        println("Ping of iot system is $ping")
    }

    // ❌ Conflicting payload declaration
    @MqttSubscribe(topic = "/home/ping", qos = AT_LEAST_ONCE)
    fun subscribe(ping: Int, temperaturePayload: TemperaturePayload) {
        // throws de.smartsquare.starter.mqtt.MqttConfigurationException
    }
}
```
