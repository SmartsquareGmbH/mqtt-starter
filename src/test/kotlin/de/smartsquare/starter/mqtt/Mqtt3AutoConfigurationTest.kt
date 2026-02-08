package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import java.math.BigDecimal
import java.math.BigInteger

@ExtendWith(EmqxExtension::class)
@SpringBootTest(classes = [JacksonAutoConfiguration::class, MqttAutoConfiguration::class, MqttSubscriberConfig::class])
class Mqtt3AutoConfigurationTest {

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var client: Mqtt3Client

    @Autowired
    private lateinit var publisher: Mqtt3Publisher

    @Test
    fun `receives byte array message`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("bytearray")
                    .payload(bytes)
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            (context.getBean<ByteArraySubscriber>().receivedPayload contentEquals bytes).shouldBeTrue()
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
            context.getBean<StringSubscriber>().receivedPayload shouldBeEqualTo "test"
        }
    }

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
            context.getBean<IntSubscriber>().receivedPayload shouldBeEqualTo 2
        }
    }

    @Test
    fun `receives long message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("long")
                    .payload("123456789".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            context.getBean<LongSubscriber>().receivedPayload shouldBeEqualTo 123456789L
        }
    }

    @Test
    fun `receives float message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("float")
                    .payload("3.14".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            context.getBean<FloatSubscriber>().receivedPayload shouldBeEqualTo 3.14f
        }
    }

    @Test
    fun `receives double message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("double")
                    .payload("3.14159".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            context.getBean<DoubleSubscriber>().receivedPayload shouldBeEqualTo 3.14159
        }
    }

    @Test
    fun `receives big integer message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("biginteger")
                    .payload("123456789012345678901234567890".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            context.getBean<BigIntegerSubscriber>().receivedPayload shouldBeEqualTo
                BigInteger("123456789012345678901234567890")
        }
    }

    @Test
    fun `receives big decimal message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("bigdecimal")
                    .payload("123.456789012345".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            context.getBean<BigDecimalSubscriber>().receivedPayload shouldBeEqualTo BigDecimal("123.456789012345")
        }
    }

    @Test
    fun `receives boolean message`() {
        client.toBlocking()
            .publish(
                Mqtt3Publish.builder()
                    .topic("boolean")
                    .payload("true".toByteArray())
                    .qos(MqttQos.EXACTLY_ONCE).build(),
            )

        await untilAssertedKluent {
            context.getBean<BooleanSubscriber>().receivedPayload shouldBeEqualTo true
        }
    }

    @Test
    fun `receives publish message`() {
        val publish = Mqtt3Publish.builder()
            .topic("publish3")
            .payload("test".toByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            context.getBean<Publish3Subscriber>().receivedPayload shouldBeEqualTo publish
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
            context.getBean<ObjectSubscriber>().receivedPayload?.value shouldBeEqualTo 3
        }
    }

    @Test
    fun `receives empty message`() {
        val publish = Mqtt3Publish.builder()
            .topic("empty")
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            context.getBean<EmptySubscriber>().receivedPayload.shouldBeTrue()
        }
    }

    @Test
    fun `receives publish message from suspend function`() {
        val publish = Mqtt3Publish.builder()
            .topic("suspend")
            .payload("1".toByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            context.getBean<SuspendSubscriber>().receivedPayload shouldBeEqualTo 1
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
            val payload = context.getBean<MultipleSubscriber>().receivedPayload.shouldNotBeNull()
            payload.topic.toString() shouldBeEqualTo "multiple"
            payload.publish shouldBeEqualTo publish
            payload.payloadString shouldBeEqualTo "12"
            payload.payloadLong shouldBeEqualTo 12
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
            context.getBean<ObjectSubscriber>().receivedPayload?.value shouldBeEqualTo 3
        }
    }

    @Test
    fun `publishes byte array message`() {
        val bytes = byteArrayOf(10, 20, 30, 40, 50)

        publisher.publish("bytearray", MqttQos.EXACTLY_ONCE, bytes)

        await untilAssertedKluent {
            (context.getBean<ByteArraySubscriber>().receivedPayload contentEquals bytes).shouldBeTrue()
        }
    }

    @Test
    fun `publishes string message`() {
        publisher.publish("string", MqttQos.EXACTLY_ONCE, "hello world")

        await untilAssertedKluent {
            context.getBean<StringSubscriber>().receivedPayload shouldBeEqualTo "hello world"
        }
    }

    @Test
    fun `publishes int message`() {
        publisher.publish("int", MqttQos.EXACTLY_ONCE, 1)

        await untilAssertedKluent {
            context.getBean<IntSubscriber>().receivedPayload shouldBeEqualTo 1
        }
    }

    @Test
    fun `publishes long message`() {
        publisher.publish("long", MqttQos.EXACTLY_ONCE, 987654321L)

        await untilAssertedKluent {
            context.getBean<LongSubscriber>().receivedPayload shouldBeEqualTo 987654321L
        }
    }

    @Test
    fun `publishes float message`() {
        publisher.publish("float", MqttQos.EXACTLY_ONCE, 2.71f)

        await untilAssertedKluent {
            context.getBean<FloatSubscriber>().receivedPayload shouldBeEqualTo 2.71f
        }
    }

    @Test
    fun `publishes double message`() {
        publisher.publish("double", MqttQos.EXACTLY_ONCE, 2.71828)

        await untilAssertedKluent {
            context.getBean<DoubleSubscriber>().receivedPayload shouldBeEqualTo 2.71828
        }
    }

    @Test
    fun `publishes big integer message`() {
        val bigInt = BigInteger("999999999999999999999999999999")

        publisher.publish("biginteger", MqttQos.EXACTLY_ONCE, bigInt)

        await untilAssertedKluent {
            context.getBean<BigIntegerSubscriber>().receivedPayload shouldBeEqualTo bigInt
        }
    }

    @Test
    fun `publishes big decimal message`() {
        val bigDec = BigDecimal("999.999999999999")

        publisher.publish("bigdecimal", MqttQos.EXACTLY_ONCE, bigDec)

        await untilAssertedKluent {
            context.getBean<BigDecimalSubscriber>().receivedPayload shouldBeEqualTo bigDec
        }
    }

    @Test
    fun `publishes boolean message`() {
        publisher.publish("boolean", MqttQos.EXACTLY_ONCE, false)

        await untilAssertedKluent {
            context.getBean<BooleanSubscriber>().receivedPayload shouldBeEqualTo false
        }
    }

    @Test
    fun `publishes object message`() {
        val obj = TemperatureMessage(42)

        publisher.publish("object", MqttQos.EXACTLY_ONCE, obj)

        await untilAssertedKluent {
            context.getBean<ObjectSubscriber>().receivedPayload?.value shouldBeEqualTo 42
        }
    }
}
