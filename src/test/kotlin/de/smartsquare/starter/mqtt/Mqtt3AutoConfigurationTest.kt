package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.IntSubscriber
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.ObjectSubscriber
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.StringSubscriber
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS

@DirtiesContext(classMode = BEFORE_CLASS)
@ExtendWith(EmqxExtension::class)
@SpringBootTest(
    classes = [
        MqttAutoConfiguration::class,
        IntSubscriber::class,
        StringSubscriber::class,
        ObjectSubscriber::class
    ]
)
class Mqtt3AutoConfigurationTest {

    @Autowired
    private lateinit var client: Mqtt3Client

    @Autowired
    private lateinit var publisher: Mqtt3Publisher

    @Autowired
    private lateinit var intSubscriber: IntSubscriber

    @Autowired
    private lateinit var stringSubscriber: StringSubscriber

    @Autowired
    private lateinit var objectSubscriber: ObjectSubscriber

    @Test
    fun `receives int message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("int")
                    .payload("2".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build()
            )

        await untilAssertedKluent {
            intSubscriber.receivedPayload shouldBeEqualTo 2
        }
    }

    @Test
    fun `receives string message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("string")
                    .payload("test".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build()
            )

        await untilAssertedKluent {
            stringSubscriber.receivedPayload shouldBeEqualTo "test"
        }
    }

    @Test
    fun `receives object message`() {
        // language=json
        val json = """
            {
              "value": 3
            }
        """.trimIndent()

        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("object")
                    .payload(json.toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build()
            )

        await untilAssertedKluent {
            objectSubscriber.receivedPayload?.value shouldBeEqualTo 3
        }
    }

    @Test
    fun `does not crash completely when sending invalid json`() {
        // language=json
        val errorJson = """
            {
              "value": 18329456734851730954
            }
        """.trimIndent()

        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("object")
                    .payload(errorJson.toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build()
            )

        // language=json
        val json = """
            {
              "value": 3
            }
        """.trimIndent()

        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("object")
                    .payload(json.toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build()
            )

        await untilAssertedKluent {
            objectSubscriber.receivedPayload?.value shouldBeEqualTo 3
        }
    }

    @Test
    fun `publishes message`() {
        publisher.publish("int", MqttQos.EXACTLY_ONCE, 1)

        await untilAssertedKluent {
            intSubscriber.receivedPayload shouldBeEqualTo 1
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
    class StringSubscriber {

        val receivedPayload get() = _receivedPayload
        private var _receivedPayload: String? = null

        @MqttSubscribe(topic = "string", qos = MqttQos.EXACTLY_ONCE)
        fun onMessage(payload: String) {
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
}
