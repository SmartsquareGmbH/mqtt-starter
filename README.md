# :honeybee: HiveMQ Spring Boot Starter

Use an automatically configured mqtt client in your Spring Boot project.

## Getting Started

### Gradle Configuration

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "de.smartsquare:mqtt-starter:0.11.0"
}
```

## Configuration

### Application Properties

```properties
# The host to connect to.
mqtt.host=test.mosquitto.org

# The port to connect to.
mqtt.port=1883

# The clientId to use when connecting (optional, random by default).
mqtt.client-id=test

# The username to use when connecting.
mqtt.username=admin

# The password to use when connecting.
mqtt.password=test

# If the connection should be encrypted.
mqtt.ssl=false

# If the session should be clean (optional, true by default).
mqtt.clean=false

# The group to use for shared subscriptions (optional).
mqtt.group=group
```

### Advanced

It is possible to additionally configure the client programmatically by implementing the `MqttClientConfigurer`
interface and exposing it as a bean.

```kotlin
@Component
class IdentifierConfigurer : MqttClientConfigurer {

    override fun configure(builder: Mqtt3ClientBuilder) {
        builder.transportConfig().mqttConnectTimeout(10, TimeUnit.SECONDS)
    }
}
```

## Usage

### Annotation based

The `MqttSubscribe` annotation is scanned on application start and receives messages on the given payload.

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

    // ✅ No parameters, for whatever reason
    @MqttSubscribe(topic = "/home/ping", qos = AT_LEAST_ONCE)
    fun subscribe() {
        println("Something happened")
    }

    // ❌ Conflicting payload declaration
    @MqttSubscribe(topic = "/home/ping", qos = AT_LEAST_ONCE)
    fun subscribe(ping: Int, temperaturePayload: TemperaturePayload) {
        // throws de.smartsquare.starter.mqtt.MqttConfigurationException
    }
}
```

### Publisher

Messages cann be published via the `MqttPublisher`.

```kotlin
import com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE
import org.springframework.stereotype.Component

@Component
class TestPublisher(private val mqttPublisher: MqttPublisher) {

    fun publish(payload: TemperaturePayload) {
        mqttPublisher.publish("/home/temperature", AT_LEAST_ONCE, payload)
    }

    class TemperaturePayload(val value: Int)
}
```

### Direct usage

The `MqttClient` is also exposed and can be used directly.

```kotlin
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class TestService(private val mqttClient: Mqtt3Client) {

    fun publishManually(payload: ByteArray): CompletableFuture<Mqtt3Publish> {
        return mqttClient.toAsync()
            .publish(
                Mqtt3Publish.builder()
                    .topic("test")
                    .payload(payload)
                    .build()
            )
    }
}
```
