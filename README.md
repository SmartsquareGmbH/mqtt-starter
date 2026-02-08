# :honeybee: HiveMQ Spring Boot Starter

Use an automatically configured mqtt 3 or 5 client in your Spring Boot project.

## Getting Started

### Gradle

```groovy
dependencies {
    implementation "de.smartsquare:mqtt-starter:0.20.0"
}
```

#### Kotlin DSL

```kotlin
dependencies {
    implementation("de.smartsquare:mqtt-starter:0.20.0")
}
```

### Maven

```xml

<dependency>
    <groupId>de.smartsquare</groupId>
    <artifactId>mqtt-starter</artifactId>
    <version>0.20.0</version>
</dependency>
```

### Compatibility Matrix

| Starter Version | Spring Boot Version |
|-----------------|---------------------|
| 0.17.0          | 2.x                 |
| 0.20.0          | 3.x                 |

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

## Advanced

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

### Object Mapping

The starter supports optional object mapping for converting MQTT message payloads to custom objects.

#### Automatic Detection

- **Jackson**: If `com.fasterxml.jackson.databind.ObjectMapper` is available, it will be used automatically.
- **Gson**: If `com.google.gson.Gson` is available, it will be used automatically.

Jackson takes precedence if both are present on the classpath.

#### Custom Object Mapper

You can provide your own custom object mapper by implementing the `MqttMessageAdapter` interface and exposing it as a
bean:

```kotlin
import de.smartsquare.starter.mqtt.mapper.MqttObjectMapper

class CustomMqttObjectMapper : MqttObjectMapper {
    override fun fromBytes(bytes: ByteArray, targetType: Class<*>): Any {
        // Your custom deserialization logic
    }

    override fun toBytes(value: Any): String {
        // Your custom serialization logic
    }
}
```

If you provide a custom `MqttMessageAdapter` bean, it will override the automatic detection and be used instead.

### Health Indicator

The starter provides a health indicator that checks the connection to the mqtt broker. If the broker is not connected,
the health indicator will return `DOWN`. The health indicator is enabled by default if actuator is on the classpath.  
It can be disabled by setting `management.health.mqtt.enabled=false`.

### GraalVM

This starter supports GraalVM out of the box. There is nothing special to do.

## Upgrade Guide

### 0.16.0 -> 0.20.0

- Spring Boot 3 and Kotlin 2.3.10 are now required.
- Jackson is now an optional dependency and no `ObjectMapper` is provided by default. If an `ObjectMapper` bean is
  found (usually the case if you have the `spring-boot-starter-json` dependency), it will be automatically used for
  object mapping. Gson is now also supported as well as the ability to provide a custom `MqttMessageAdapter` bean.
- There is no direct dependency on Spring Boot libraries anymore to avoid version conflicts.

### 0.15.0 -> 0.16.0

- `mqtt.host` and `mqtt.port` now must be set explicitly. The default value of localhost:1883 has been removed.
- Graceful shutdown is now supported and by default enabled. That has the side effect that messages are delivered on a
  different thread pool. The application may also take more time to shut down. Graceful shutdown can be turned off
  with `mqtt.shutdown=immediate`.
