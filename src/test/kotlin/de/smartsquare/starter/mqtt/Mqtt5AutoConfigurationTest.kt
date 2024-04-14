package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import de.smartsquare.starter.mqtt.Mqtt5AutoConfigurationTest.ErrorSubscriber
import de.smartsquare.starter.mqtt.Mqtt5AutoConfigurationTest.IntSubscriber
import de.smartsquare.starter.mqtt.Mqtt5AutoConfigurationTest.PublishSubscriber
import de.smartsquare.starter.mqtt.Mqtt5AutoConfigurationTest.SuspendSubscriber
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.TestPropertySource
import java.time.Duration

@ExtendWith(EmqxExtension::class)
@SpringBootTest(
    classes = [
        MqttAutoConfiguration::class,
        IntSubscriber::class,
        PublishSubscriber::class,
        SuspendSubscriber::class,
        ErrorSubscriber::class,
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
    private lateinit var publishSubscriber: PublishSubscriber

    @Autowired
    private lateinit var suspendSubscriber: SuspendSubscriber

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
    fun `receives publish message`() {
        val publish = Mqtt5Publish.builder()
            .topic("string")
            .payload("test".toByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            publishSubscriber.receivedPayload shouldBeEqualTo publish
        }
    }

    @Test
    fun `receives publish message from suspend function`() {
        val publish = Mqtt5Publish.builder()
            .topic("suspend")
            .payload("test".toByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            suspendSubscriber.receivedPayload shouldBeEqualTo publish
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

    @Test
    fun `publishes message with expiry`() {
        publisher.publish("int", MqttQos.EXACTLY_ONCE, 1, Duration.ofSeconds(30))

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
    class PublishSubscriber {

        val receivedPayload get() = _receivedPayload
        private var _receivedPayload: Mqtt5Publish? = null

        @MqttSubscribe(topic = "string", qos = MqttQos.EXACTLY_ONCE)
        fun onMessage(payload: Mqtt5Publish) {
            _receivedPayload = payload
        }
    }

    @Component
    class SuspendSubscriber {

        val receivedPayload get() = _receivedPayload
        private var _receivedPayload: Mqtt5Publish? = null

        @MqttSubscribe(topic = "suspend", qos = MqttQos.EXACTLY_ONCE)
        suspend fun onMessage(payload: Mqtt5Publish) {
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
