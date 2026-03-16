package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import de.smartsquare.starter.mqtt.Mqtt5Publisher.PublishingOptions
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource
import kotlin.jvm.optionals.getOrNull

@AutoConfigureJson
@ExtendWith(EmqxExtension::class)
@SpringBootTest(classes = [JacksonAutoConfiguration::class, MqttAutoConfiguration::class, MqttSubscriberConfig::class])
@TestPropertySource(properties = ["mqtt.version=5"])
class Mqtt5AutoConfigurationTest {

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var client: Mqtt5Client

    @Autowired
    private lateinit var publisher: Mqtt5Publisher

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
            context.getBean<IntSubscriber>().receivedPayload shouldBeEqualTo 2
        }
    }

    @Test
    fun `receives publish message`() {
        val publish = Mqtt5Publish.builder()
            .topic("publish5")
            .payload("test".toByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            context.getBean<Publish5Subscriber>().receivedPayload shouldBeEqualTo publish
        }
    }

    @Test
    fun `receives publish message from suspend function`() {
        val publish = Mqtt5Publish.builder()
            .topic("suspend")
            .payload("1".toByteArray())
            .qos(MqttQos.EXACTLY_ONCE).build()

        client.toBlocking().publish(publish)

        await untilAssertedKluent {
            context.getBean<SuspendSubscriber>().receivedPayload shouldBeEqualTo 1
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
            context.getBean<ErrorSubscriber>().payloadSum shouldBeEqualTo 3
        }
    }

    @Test
    fun `publishes message`() {
        publisher.publish("int", MqttQos.EXACTLY_ONCE, 1)

        await untilAssertedKluent {
            context.getBean<IntSubscriber>().receivedPayload shouldBeEqualTo 1
        }
    }

    @Test
    fun `publishes message with expiry`() {
        publisher.publish("publish5", MqttQos.EXACTLY_ONCE, "1", PublishingOptions(messageExpiryInterval = 10))

        await untilAssertedKluent {
            context.getBean<Publish5Subscriber>().receivedPayload?.messageExpiryInterval?.asLong shouldBeEqualTo 10
        }
    }

    @Test
    fun `publishes message with contentType`() {
        publisher.publish("publish5", MqttQos.EXACTLY_ONCE, "1", PublishingOptions(contentType = "text/plain"))

        await untilAssertedKluent {
            val contentTypeOption = context.getBean<Publish5Subscriber>().receivedPayload?.contentType
            val contentType = contentTypeOption?.getOrNull()?.toString()
            contentType.shouldNotBeNull()
            contentType shouldBeEqualTo "text/plain"
        }
    }
}
