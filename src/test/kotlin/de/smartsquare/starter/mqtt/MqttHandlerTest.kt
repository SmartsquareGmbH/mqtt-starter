package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

object TestMqttSubscriberCollector {
    operator fun invoke(bean: Any) = MqttSubscriberCollector(TestObjectProvider(MqttProperties())).apply {
        postProcessAfterInitialization(bean, "testBean")
    }
}

@Suppress("RedundantSuspendModifier")
class MqttHandlerTest {

    private val mapper = jsonMapper { findAndAddModules() }
    private val adapter = DefaultMqttMessageAdapter(mapper)
    private val messageErrorHandler = MqttMessageErrorHandler()

    @Test
    fun `invoke correct method for multiple subscriber methods`() {
        val subscriber = object {
            var invoked = false

            @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
            fun test() {
                invoked = true
            }

            @MqttSubscribe("test2", qos = MqttQos.EXACTLY_ONCE)
            fun test2() = Unit
        }

        val collector = TestMqttSubscriberCollector(subscriber)
        collector.subscribers.size shouldBeEqualTo 2
        val handler = MqttHandler(collector, adapter, messageErrorHandler)

        val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
        handler.handle(Mqtt5PublishContainer(publish))

        subscriber.invoked shouldBeEqualTo true
    }

    @Nested
    inner class RegularMethodInvocation {
        @Test
        fun `invokes parameterless subscriber method`() {
            val subscriber = object {
                var invoked = false

                @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
                fun test() {
                    invoked = true
                }
            }

            val collector = TestMqttSubscriberCollector(subscriber)
            val handler = MqttHandler(collector, adapter, messageErrorHandler)

            val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
            handler.handle(Mqtt5PublishContainer(publish))

            subscriber.invoked shouldBeEqualTo true
        }

        @Test
        fun `invokes string subscriber method`() {
            val subscriber = object {
                lateinit var invoked: String

                @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
                fun test(data: String) {
                    invoked = data
                }
            }

            val collector = TestMqttSubscriberCollector(subscriber)
            val handler = MqttHandler(collector, adapter, messageErrorHandler)

            val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
            handler.handle(Mqtt5PublishContainer(publish))

            subscriber.invoked shouldBeEqualTo "1"
        }

        @Test
        fun `invokes byte subscriber method`() {
            val subscriber = object {
                lateinit var invoked: ByteArray

                @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
                fun test(data: ByteArray) {
                    invoked = data
                }
            }

            val collector = TestMqttSubscriberCollector(subscriber)
            val handler = MqttHandler(collector, adapter, messageErrorHandler)

            val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
            handler.handle(Mqtt5PublishContainer(publish))

            subscriber.invoked shouldBeEqualTo "1".encodeToByteArray()
        }

        @Test
        fun `invokes object subscriber method`() {
            val subscriber = object {
                lateinit var invoked: TemperatureMessage

                @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
                fun test(data: TemperatureMessage) {
                    invoked = data
                }
            }

            val collector = TestMqttSubscriberCollector(subscriber)
            val handler = MqttHandler(collector, adapter, messageErrorHandler)

            val obj = TemperatureMessage(1)
            val publish = Mqtt5Publish.builder().topic("test").payload(mapper.writeValueAsBytes(obj)).build()
            handler.handle(Mqtt5PublishContainer(publish))

            subscriber.invoked shouldBeEqualTo obj
        }
    }

    @Nested
    inner class SuspendingMethodInvocation {
        @Test
        fun `invokes suspend subscriber method`() {
            val subscriber = object {
                var invoked = false

                @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
                suspend fun test() {
                    invoked = true
                }
            }

            val collector = TestMqttSubscriberCollector(subscriber)
            val handler = MqttHandler(collector, adapter, messageErrorHandler)

            val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
            handler.handle(Mqtt5PublishContainer(publish))

            subscriber.invoked shouldBeEqualTo true
        }

        @Test
        fun `invokes suspend string subscriber method`() {
            val subscriber = object {
                lateinit var invoked: String

                @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
                suspend fun test(data: String) {
                    invoked = data
                }
            }

            val collector = TestMqttSubscriberCollector(subscriber)
            val handler = MqttHandler(collector, adapter, messageErrorHandler)

            val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
            handler.handle(Mqtt5PublishContainer(publish))

            subscriber.invoked shouldBeEqualTo "1"
        }

        @Test
        fun `invokes suspend byte subscriber method`() {
            val subscriber = object {
                lateinit var invoked: ByteArray

                @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
                suspend fun test(data: ByteArray) {
                    invoked = data
                }
            }

            val collector = TestMqttSubscriberCollector(subscriber)
            val handler = MqttHandler(collector, adapter, messageErrorHandler)

            val publish = Mqtt5Publish.builder().topic("test").payload("1".encodeToByteArray()).build()
            handler.handle(Mqtt5PublishContainer(publish))

            subscriber.invoked shouldBeEqualTo "1".encodeToByteArray()
        }

        @Test
        fun `invokes suspend object subscriber method`() {
            val subscriber = object {
                lateinit var invoked: TemperatureMessage

                @MqttSubscribe("test", qos = MqttQos.EXACTLY_ONCE)
                suspend fun test(data: TemperatureMessage) {
                    invoked = data
                }
            }

            val collector = TestMqttSubscriberCollector(subscriber)
            val handler = MqttHandler(collector, adapter, messageErrorHandler)

            val obj = TemperatureMessage(1)
            val publish = Mqtt5Publish.builder().topic("test").payload(mapper.writeValueAsBytes(obj)).build()
            handler.handle(Mqtt5PublishContainer(publish))

            subscriber.invoked shouldBeEqualTo obj
        }
    }
}
