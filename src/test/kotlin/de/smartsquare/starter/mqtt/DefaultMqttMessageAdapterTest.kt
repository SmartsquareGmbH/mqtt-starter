package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class DefaultMqttMessageAdapterTest {

    private val adapter = DefaultMqttMessageAdapter(jacksonObjectMapper())

    @Test
    fun `should adapt int message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), Int::class.java)
        result.shouldBeInstanceOf<Int>()
        result shouldBeEqualTo 1
    }

    @Test
    fun `should adapt string message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), String::class.java)
        result.shouldBeInstanceOf<String>()
        result shouldBeEqualTo "1"
    }

    @Test
    fun `should adapt byte array message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), ByteArray::class.java)
        result.shouldBeInstanceOf<ByteArray>()
        (result as ByteArray) shouldBeEqualTo "1".encodeToByteArray()
    }

    @Test
    fun `should adapt object message`() {
        val obj = TemperatureMessage(1)
        val publish = Mqtt5Publish.builder().topic("test").payload(jacksonObjectMapper().writeValueAsBytes(obj)).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), TemperatureMessage::class.java)
        result.shouldBeInstanceOf<TemperatureMessage>()
        result shouldBeEqualTo obj
    }

    @Test
    fun `should adapt publish message`() {
        val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
        val result = adapter.adapt(Mqtt5PublishContainer(publish), Mqtt5Publish::class.java)
        result.shouldBeInstanceOf<Mqtt5Publish>()
        result shouldBeEqualTo publish
    }

    @Test
    fun `should adapt int payload`() {
        val result = adapter.adapt(1)

        result.decodeToString() shouldBeEqualTo "1"
    }

    @Test
    fun `should adapt string payload`() {
        val result = adapter.adapt("test")

        result.decodeToString() shouldBeEqualTo "test"
    }

    @Test
    fun `should adapt object payload`() {
        val obj = TemperatureMessage(1)

        val result = adapter.adapt(obj)

        // language=json
        result.decodeToString() shouldBeEqualTo """{"value":1}"""
    }
}
