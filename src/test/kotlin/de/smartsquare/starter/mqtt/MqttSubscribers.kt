package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger

@Component
class MqttTopicSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: MqttTopic? = null

    @MqttSubscribe(topic = "topic", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: MqttTopic) {
        _receivedPayload = payload
    }
}

@Component
class ByteArraySubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: ByteArray? = null

    @MqttSubscribe(topic = "bytearray", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: ByteArray) {
        _receivedPayload = payload
    }
}

@Component
class StringSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: String? = null

    @MqttSubscribe(topic = "string", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: String) {
        _receivedPayload = payload
    }
}

@Component
class IntSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Int? = null

    @MqttSubscribe(topic = "int", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: Int) {
        _receivedPayload = payload
    }
}

@Component
class LongSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Long? = null

    @MqttSubscribe(topic = "long", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: Long) {
        _receivedPayload = payload
    }
}

@Component
class FloatSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Float? = null

    @MqttSubscribe(topic = "float", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: Float) {
        _receivedPayload = payload
    }
}

@Component
class DoubleSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Double? = null

    @MqttSubscribe(topic = "double", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: Double) {
        _receivedPayload = payload
    }
}

@Component
class BigIntegerSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: BigInteger? = null

    @MqttSubscribe(topic = "biginteger", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: BigInteger) {
        _receivedPayload = payload
    }
}

@Component
class BigDecimalSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: BigDecimal? = null

    @MqttSubscribe(topic = "bigdecimal", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: BigDecimal) {
        _receivedPayload = payload
    }
}

@Component
class BooleanSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Boolean? = null

    @MqttSubscribe(topic = "boolean", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: Boolean) {
        _receivedPayload = payload
    }
}

@Component
class Publish3Subscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Mqtt3Publish? = null

    @MqttSubscribe(topic = "publish3", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: Mqtt3Publish) {
        _receivedPayload = payload
    }
}

@Component
class Publish5Subscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Mqtt5Publish? = null

    @MqttSubscribe(topic = "publish5", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: Mqtt5Publish) {
        _receivedPayload = payload
    }
}

@Component
class ObjectSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: TemperatureMessage? = null

    @MqttSubscribe(topic = "object", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: TemperatureMessage) {
        _receivedPayload = payload
    }
}

@Component
class ShareSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Boolean = false

    @MqttSubscribe(topic = "shared", qos = MqttQos.EXACTLY_ONCE, shared = true)
    fun onMessage() {
        _receivedPayload = true
    }
}

@Component
class EmptySubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Boolean = false

    @MqttSubscribe(topic = "empty", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage() {
        _receivedPayload = true
    }
}

@Component
class SuspendSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: Int? = null

    @MqttSubscribe(topic = "suspend", qos = MqttQos.EXACTLY_ONCE)
    suspend fun onMessage(payload: Int) {
        _receivedPayload = payload
    }
}

@Component
class MultipleSubscriber {

    val receivedPayload get() = _receivedPayload
    private var _receivedPayload: MultiplePayload? = null

    @MqttSubscribe(topic = "multiple", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(topic: MqttTopic, publish: Mqtt3Publish, payloadString: String, payloadLong: Long) {
        _receivedPayload = MultiplePayload(topic, publish, payloadString, payloadLong)
    }

    data class MultiplePayload(
        val topic: MqttTopic,
        val publish: Mqtt3Publish,
        val payloadString: String,
        val payloadLong: Long,
    )
}

@Component
class ErrorSubscriber {

    val payloadSum get() = _payloadSum
    private var _payloadSum: Int = 0

    @MqttSubscribe(topic = "error", qos = MqttQos.EXACTLY_ONCE)
    fun onMessage(payload: Int) {
        require(payload >= 0) { "Synthetic validation error: $payload is less than 0" }

        _payloadSum += payload
    }
}

@Import(
    MqttTopicSubscriber::class,
    ByteArraySubscriber::class,
    StringSubscriber::class,
    IntSubscriber::class,
    LongSubscriber::class,
    FloatSubscriber::class,
    DoubleSubscriber::class,
    BigIntegerSubscriber::class,
    BigDecimalSubscriber::class,
    BooleanSubscriber::class,
    Publish3Subscriber::class,
    Publish5Subscriber::class,
    ObjectSubscriber::class,
    EmptySubscriber::class,
    SuspendSubscriber::class,
    MultipleSubscriber::class,
    ErrorSubscriber::class,
)
@Configuration
class MqttSubscriberConfig
