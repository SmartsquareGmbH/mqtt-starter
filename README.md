# :honeybee: HiveMQ Spring Boot Starter

Use an automatically configured mqtt 3 or 5 client in your Spring Boot project.

## Getting Started

### Gradle Configuration

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "de.smartsquare:mqtt-starter:0.17.0"
}
```

## Configuration

### Application Properties

The main configuration mechanism is via properties. `mqtt.host` and `mqtt.port` are required.

```properties
# The host to connect to.
mqtt.host=test.mosquitto.org
# The port to connect to.
mqtt.port=1883
# The clientId to use when connecting (random by default).
mqtt.client-id=test
# The session expiry interval in seconds, has to be in [0, 4294967295] (0 by default). Only for mqtt 5.
mqtt.session-expiry=0
# The username to use when connecting.
mqtt.username=admin
# The password to use when connecting.
mqtt.password=test
# If the connection should be encrypted.
mqtt.ssl=false
# If the session should be clean (true by default).
mqtt.clean=false
# The group to use for shared subscriptions.
mqtt.group=group
# The mqtt protocol version to use. 3 and 5 are supported (3 by default).
mqtt.version=3
# Disable or enable the mqtt client. Note that no beans are available to be injected if disabled.
mqtt.enabled=true
```

### Advanced

It is possible to additionally configure the client programmatically by implementing either the `Mqtt3ClientConfigurer`
or `Mqtt5ClientConfigurer` interface and exposing it as a bean.

```kotlin
@Component
class MqttTimeoutConfigurer : MqttClientConfigurer {

    override fun configure(builder: Mqtt3ClientBuilder) {
        builder.transportConfig().mqttConnectTimeout(10, TimeUnit.SECONDS)
    }
}
```

## Usage

### Annotation based

The `MqttSubscribe` annotation is scanned on application start and receives messages on the given topic.  
It additionally supports kotlin suspend functions. Those functions are run inside the mqtt client thread pool.

```kotlin
import com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import de.smartsquare.starter.mqtt.MqttSubscribe
import org.springframework.stereotype.Component

@Component
class TestConsumer {

    // Topic and payload
    @MqttSubscribe(topic = "/home/+/temperature", qos = AT_LEAST_ONCE)
    fun subscribe(payload: TemperaturePayload, topic: MqttTopic) {
        println("Temperature is ${payload.value} °C in room ${topic.levels[1]}]")
    }

    class TemperaturePayload(val value: Int)

    // Only topic
    @MqttSubscribe(topic = "/home/+/light/on", qos = AT_LEAST_ONCE)
    fun subscribe(topic: MqttTopic) {
        println("Light turned on in room ${topic.levels[1]}]")
    }

    // Only numeric payload
    @MqttSubscribe(topic = "/home/ping", qos = AT_LEAST_ONCE)
    fun subscribe(ping: Int) {
        println("Ping of iot system is $ping")
    }

    // Raw message
    @MqttSubscribe(topic = "/home/ping", qos = AT_LEAST_ONCE)
    fun subscribe(message: Mqtt3Publish) {
        println("Raw payload: ${message.payloadAsBytes.decodeToString()}")
    }

    // No parameters
    @MqttSubscribe(topic = "/home/ping", qos = AT_LEAST_ONCE)
    fun subscribe() {
        println("Something happened")
    }

  // Suspending function
  @MqttSubscribe(topic = "/home/ping", qos = AT_LEAST_ONCE)
  suspend fun suspending() {
    println("Something happened suspending")
  }
}
```

### Publisher

Messages can be published via the `Mqtt3Publisher` or `Mqtt5Publisher`.

```kotlin
import com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE
import org.springframework.stereotype.Component

@Component
class TestPublisher(private val mqttPublisher: Mqtt3Publisher) {

    fun publish(payload: TemperaturePayload) {
        mqttPublisher.publish("/home/temperature", AT_LEAST_ONCE, payload)
    }

    class TemperaturePayload(val value: Int)
}
```

### Direct usage

Depending on the version, an `Mqtt3Client` or `Mqtt5Client` is also exposed and can be used directly.

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

### GraalVM

This starter supports GraalVM out of the box. There is nothing special to do.

### Upgrade Guide

#### 0.15.0 -> 0.16.0

- `mqtt.host` and `mqtt.port` now must be set explicitly. The default value of localhost:1883 has been removed.
- Graceful shutdown is now supported and by default enabled. That has the side effect that messages are delivered on a
  different thread pool. The application may also take more time to shut down. Graceful shutdown can be turned off
  with `mqtt.shutdown=immediate`.
