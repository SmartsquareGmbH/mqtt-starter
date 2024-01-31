package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.TestPropertySource

@ExtendWith(EmqxExtension::class)
@SpringBootTest(
    classes = [
        MqttAutoConfiguration::class,
        Mqtt5AutoConfigurationTest.IntSubscriber::class,
        Mqtt5AutoConfigurationTest.ErrorSubscriber::class,
    ],
)
@TestPropertySource(properties = ["mqtt.version=5"])
class Mqtt5AutoConfigurationTest {

    @Autowired
    private lateinit var client: Mqtt5Client

    @Autowired
    private lateinit var publisher: Mqtt5Publisher

    @Autowired
    private lateinit var intSubscriber: IntSubscriber

    @Autowired
    private lateinit var errorSubscriber: ErrorSubscriber

    @Test
    fun `receives int message`() {
        client.toBlocking()
            .publish(
                Mqtt5Publish.builder()
                    .topic("int")
                    .payload("2".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            intSubscriber.receivedPayload shouldBeEqualTo 2
        }
    }

    @Test
    fun `does not crash completely when subscriber throws exception`() {
        client.toBlocking()
            .publish(
                Mqtt5Publish.builder()
                    .topic("error")
                    .payload("-1".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        client.toBlocking()
            .publish(
                Mqtt5Publish.builder()
                    .topic("error")
                    .payload("3".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            errorSubscriber.payloadSum shouldBeEqualTo 3
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
    class ErrorSubscriber {

        val payloadSum get() = _payloadSum
        private var _payloadSum: Int = 0

        @MqttSubscribe(topic = "error", qos = MqttQos.EXACTLY_ONCE)
        fun onMessage(payload: Int) {
            require(payload >= 0) { "Synthetic validation error: $payload is less than 0" }

            _payloadSum += payload
        }
    }
}
