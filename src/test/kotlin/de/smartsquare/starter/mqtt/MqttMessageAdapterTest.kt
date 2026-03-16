package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import de.smartsquare.starter.mqtt.mapper.JacksonMqttObjectMapper
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.math.BigInteger

class MqttMessageAdapterTest {

    private val mapper = JsonMapper()
    private val adapter = MqttMessageAdapter(JacksonMqttObjectMapper(mapper))

    @Test
    fun `should adapt byte array message`() {
        val byteArray = byteArrayOf(1, 2, 3)
        val publish = Mqtt5Publish.builder().topic("test").payload(byteArray).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), ByteArray::class.java)
        result.shouldBeInstanceOf<ByteArray>().contentEquals(byteArray).shouldBeTrue()
    }

    @Test
    fun `should adapt string message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), String::class.java)
        result.shouldBeInstanceOf<String>() shouldBeEqualTo "1"
    }

    @Test
    fun `should adapt int message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), Int::class.java)
        result.shouldBeInstanceOf<Int>() shouldBeEqualTo 1
    }

    @Test
    fun `should adapt long message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("123456789".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), Long::class.java)
        result.shouldBeInstanceOf<Long>() shouldBeEqualTo 123456789L
    }

    @Test
    fun `should adapt float message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1.5".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), Float::class.java)
        result.shouldBeInstanceOf<Float>() shouldBeEqualTo 1.5f
    }

    @Test
    fun `should adapt double message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1.5".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), Double::class.java)
        result.shouldBeInstanceOf<Double>() shouldBeEqualTo 1.5
    }

    @Test
    fun `should adapt big integer message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("12345678901234567890".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), BigInteger::class.java)
        result.shouldBeInstanceOf<BigInteger>() shouldBeEqualTo BigInteger("12345678901234567890")
    }

    @Test
    fun `should adapt big decimal message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("123.456".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), BigDecimal::class.java)
        result.shouldBeInstanceOf<BigDecimal>() shouldBeEqualTo BigDecimal("123.456")
    }

    @Test
    fun `should adapt boolean message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("true".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), Boolean::class.java)
        result.shouldBeInstanceOf<Boolean>() shouldBeEqualTo true
    }

    @Test
    fun `should adapt mqtt topic message`() {
        val publish = Mqtt5Publish.builder().topic("test/topic").payload("1".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), MqttTopic::class.java)
        result.shouldBeInstanceOf<MqttTopic>().toString() shouldBeEqualTo "test/topic"
    }

    @Test
    fun `should adapt object message`() {
        val obj = TemperatureMessage(1)
        val publish = Mqtt5Publish.builder().topic("test").payload(mapper.writeValueAsBytes(obj)).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), TemperatureMessage::class.java)
        result.shouldBeInstanceOf<TemperatureMessage>() shouldBeEqualTo obj
    }

    @Test
    fun `should adapt publish message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), Mqtt5Publish::class.java)
        result.shouldBeInstanceOf<Mqtt5Publish>() shouldBeEqualTo publish
    }

    @Test
    fun `should adapt string payload`() {
        val result = adapter.adapt("test")
        result.decodeToString() shouldBeEqualTo "test"
    }

    @Test
    fun `should adapt byte array payload`() {
        val byteArray = byteArrayOf(1, 2, 3)
        val result = adapter.adapt(byteArray)
        result.contentEquals(byteArray).shouldBeTrue()
    }

    @Test
    fun `should adapt int payload`() {
        val result = adapter.adapt(1)
        result.decodeToString() shouldBeEqualTo "1"
    }

    @Test
    fun `should adapt long payload`() {
        val result = adapter.adapt(123456789L)
        result.decodeToString() shouldBeEqualTo "123456789"
    }

    @Test
    fun `should adapt float payload`() {
        val result = adapter.adapt(1.5f)
        result.decodeToString() shouldBeEqualTo "1.5"
    }

    @Test
    fun `should adapt double payload`() {
        val result = adapter.adapt(1.5)
        result.decodeToString() shouldBeEqualTo "1.5"
    }

    @Test
    fun `should adapt big integer payload`() {
        val result = adapter.adapt(BigInteger("12345678901234567890"))
        result.decodeToString() shouldBeEqualTo "12345678901234567890"
    }

    @Test
    fun `should adapt big decimal payload`() {
        val result = adapter.adapt(BigDecimal("123.456"))
        result.decodeToString() shouldBeEqualTo "123.456"
    }

    @Test
    fun `should adapt boolean payload`() {
        val result = adapter.adapt(true)
        result.decodeToString() shouldBeEqualTo "true"
    }

    @Test
    fun `should adapt object payload`() {
        val obj = TemperatureMessage(1)
        val result = adapter.adapt(obj)

        // language=json
        JSONAssert.assertEquals(result.decodeToString(), """{ "value": 1 }""", true)
    }
}
