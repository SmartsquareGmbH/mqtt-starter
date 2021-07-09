package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.datatypes.MqttTopic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class MqttMessageAdapterTest {

    private val adapter = MqttMessageAdapter(jacksonObjectMapper())

    @Test
    fun `should adapt int message`() {
        val result = adapter.adapt(MqttTopic.of("test"), "1".encodeToByteArray(), Int::class.java)
        result.shouldBeInstanceOf<Int>()
        result shouldBeEqualTo 1
    }

    @Test
    fun `should adapt string message`() {
        val result = adapter.adapt(MqttTopic.of("test"), "1".encodeToByteArray(), String::class.java)
        result.shouldBeInstanceOf<String>()
        result shouldBeEqualTo "1"
    }

    @Test
    fun `should adapt object message`() {
        val obj = TemperatureMessage(1)
        val payload = jacksonObjectMapper().writeValueAsBytes(obj)

        val result = adapter.adapt(MqttTopic.of("test"), payload, TemperatureMessage::class.java)
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
