package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class MqttMessageAdapterTest {

    private val adapter = MqttMessageAdapter(jacksonObjectMapper())

    @Test
    fun `should adapt int message`() {
        val message = Mqtt3Publish.builder()
            .topic("test")
            .payload("1".encodeToByteArray())
            .build()

        val result = adapter.adapt(message, Int::class.java)
        result.shouldBeInstanceOf<Int>()
        result shouldBeEqualTo 1
    }

    @Test
    fun `should adapt string message`() {
        val message = Mqtt3Publish.builder()
            .topic("test")
            .payload("1".encodeToByteArray())
            .build()

        val result = adapter.adapt(message, String::class.java)
        result.shouldBeInstanceOf<String>()
        result shouldBeEqualTo "1"
    }

    @Test
    fun `should adapt object message`() {
        val obj = TemperatureMessage(1)

        val message = Mqtt3Publish.builder()
            .topic("test")
            .payload(jacksonObjectMapper().writeValueAsBytes(obj))
            .build()

        val result = adapter.adapt(message, TemperatureMessage::class.java)
        result.shouldBeInstanceOf<TemperatureMessage>()
        result shouldBeEqualTo obj
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
