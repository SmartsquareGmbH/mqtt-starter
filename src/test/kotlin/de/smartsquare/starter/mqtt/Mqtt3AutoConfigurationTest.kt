package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.EmptySubscriber
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.IntSubscriber
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.MultipleSubscriber
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.ObjectSubscriber
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.PublishSubscriber
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.StringSubscriber
import de.smartsquare.starter.mqtt.Mqtt3AutoConfigurationTest.SuspendSubscriber
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component

@ExtendWith(EmqxExtension::class)
@SpringBootTest(
    classes = [
        MqttAutoConfiguration::class,
        IntSubscriber::class,
        StringSubscriber::class,
        ObjectSubscriber::class,
        PublishSubscriber::class,
        SuspendSubscriber::class,
        EmptySubscriber::class,
        MultipleSubscriber::class,
    ],
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

    @Autowired
    private lateinit var publishSubscriber: PublishSubscriber

    @Autowired
    private lateinit var suspendSubscriber: SuspendSubscriber

    @Autowired
    private lateinit var emptySubscriber: EmptySubscriber

    @Autowired
    private lateinit var multipleSubscriber: MultipleSubscriber

    @Test
    fun `receives int message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("int")
                    .payload("2".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
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
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            stringSubscriber.receivedPayload shouldBeEqualTo "test"
        }
    }

    @Test
    fun `receives publish message`() {
        val publish = Mqtt3Publish.builder()
            .topic("publish")
            .payload("test".toByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            publishSubscriber.receivedPayload shouldBeEqualTo publish
        }
    }

    @Test
    fun `receives publish message from suspend function`() {
        val publish = Mqtt3Publish.builder()
            .topic("suspend")
            .payload("test".toByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            suspendSubscriber.receivedPayload shouldBeEqualTo publish
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
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            objectSubscriber.receivedPayload?.value shouldBeEqualTo 3
        }
    }

    @Test
    fun `receives empty message`() {
        val publish = Mqtt3Publish.builder()
            .topic("empty")
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            emptySubscriber.receivedPayload.shouldBeTrue()
        }
    }

    @Test
    fun `receives multiple message`() {
        val publish = Mqtt3Publish.builder()
            .topic("multiple")
            .payload("12".encodeToByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            multipleSubscriber.receivedPayload.shouldNotBeNull()
            multipleSubscriber.receivedPayload!!.topic.toString() shouldBeEqualTo "multiple"
            multipleSubscriber.receivedPayload!!.publish shouldBeEqualTo publish
            multipleSubscriber.receivedPayload!!.payloadString shouldBeEqualTo "12"
            multipleSubscriber.receivedPayload!!.payloadLong shouldBeEqualTo 12
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
                    .qos(MqttQos.EXACTLY_ONCE).build(),
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
                    .qos(MqttQos.EXACTLY_ONCE).build(),
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
    class PublishSubscriber {

        val receivedPayload get() = _receivedPayload
        private var _receivedPayload: Mqtt3Publish? = null

        @MqttSubscribe(topic = "publish", qos = MqttQos.EXACTLY_ONCE)
        fun onMessage(payload: Mqtt3Publish) {
            _receivedPayload = payload
        }
    }

    @Component
    class SuspendSubscriber {

        val receivedPayload get() = _receivedPayload
        private var _receivedPayload: Mqtt3Publish? = null

        @MqttSubscribe(topic = "suspend", qos = MqttQos.EXACTLY_ONCE)
        suspend fun onMessage(payload: Mqtt3Publish) {
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
    class EmptySubscriber {

        val receivedPayload get() = _receivedPayload
        private var _receivedPayload: Boolean = false

        @MqttSubscribe(topic = "empty", qos = MqttQos.EXACTLY_ONCE)
        fun onMessage() {
            _receivedPayload = true
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
}
