package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import de.smartsquare.starter.mqtt.MqttIntegrationTests.IntSubscriber
import de.smartsquare.starter.mqtt.MqttIntegrationTests.ObjectSubscriber
import de.smartsquare.starter.mqtt.MqttIntegrationTests.StringSubscriber
import org.awaitility.kotlin.await
import org.awaitility.kotlin.has
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootTest(
    classes = [
        MqttAutoConfiguration::class,
        IntSubscriber::class,
        StringSubscriber::class,
        ObjectSubscriber::class
    ]
)
class MqttIntegrationTests {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private val logConsumer get() = Slf4jLogConsumer(logger).withSeparateOutputStreams()

        private val emqxImageName = DockerImageName.parse("emqx/emqx:4.2.10")

        private val emqx = KGenericContainer(emqxImageName)
            .withEnv("EMQX_LOADED_PLUGINS", "emqx_auth_mnesia")
            .withEnv("EMQX_AUTH__MNESIA__AS", "username")
            .withEnv("EMQX_ALLOW_ANONYMOUS", "false")
            .withEnv("WAIT_FOR_ERLANG", "60")
            .withClasspathResourceMapping(
                "acl.conf",
                "/opt/emqx/etc/acl.conf",
                BindMode.READ_ONLY
            )
            .withClasspathResourceMapping(
                "mnesia.conf",
                "/opt/emqx/etc/plugins/emqx_auth_mnesia.conf",
                BindMode.READ_ONLY
            )
            .withExposedPorts(1883, 8081, 18083)
            .waitingFor(Wait.forLogMessage(".*is running now!.*", 1))
            .withLogConsumer(logConsumer.withPrefix("emqx"))

        init {
            emqx.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun brokerProperties(registry: DynamicPropertyRegistry) {
            registry.add("mqtt.host") { emqx.host }
            registry.add("mqtt.port") { emqx.firstMappedPort }
            registry.add("mqtt.clientId") { "test" }
            registry.add("mqtt.username") { "admin" }
            registry.add("mqtt.password") { "public" }
            registry.add("mqtt.ssl") { false }
        }
    }

    @Autowired
    private lateinit var client: Mqtt3Client

    @Autowired
    private lateinit var publisher: MqttPublisher

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

        await untilCallTo { intSubscriber.receivedPayload } has { this == 2 }
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

        await untilCallTo { stringSubscriber.receivedPayload } has { this == "test" }
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

        await untilCallTo { objectSubscriber.receivedPayload } has { value == 3 }
    }

    @Test
    fun `publishes message`() {
        publisher.publish("int", MqttQos.EXACTLY_ONCE, 1)

        await untilCallTo { intSubscriber.receivedPayload } has { this == 1 }
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