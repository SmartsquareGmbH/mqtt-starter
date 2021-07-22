package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import com.hivemq.client.mqtt.datatypes.MqttTopic
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldNotThrow
import org.amshove.kluent.shouldStartWith
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test

class AnnotationCollectorTests {

    private val annotationCollector = AnnotationCollector()

    @Test
    fun `passes if only payload is defined`() {
        val bean = object {
            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage(a: String) = Unit
        }

        invoking { annotationCollector.postProcessBeforeInitialization(bean, "testBean") }
            .shouldNotThrow(MqttConfigurationException::class)
    }

    @Test
    fun `passes if only topic is defined`() {
        val bean = object {
            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage(t: MqttTopic) = Unit
        }

        invoking { annotationCollector.postProcessBeforeInitialization(bean, "testBean") }
            .shouldNotThrow(MqttConfigurationException::class)
    }

    @Test
    fun `passes if payload and topic is defined`() {
        val bean = object {
            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage(a: String, topic: MqttTopic) = Unit
        }

        invoking { annotationCollector.postProcessBeforeInitialization(bean, "testBean") }
            .shouldNotThrow(MqttConfigurationException::class)
    }

    @Test
    fun `passes if a subscriber has no parameters`() {
        val bean = object {
            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage() = Unit
        }

        invoking { annotationCollector.postProcessBeforeInitialization(bean, "testBean") }
            .shouldNotThrow(MqttConfigurationException::class)
    }

    @Test
    fun `throws if one subscriber has more than one payload parameter`() {
        val bean = object {
            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage(a: String, b: String) = Unit
        }

        invoking { annotationCollector.postProcessAfterInitialization(bean, "testBean") }
            .shouldThrow(MqttConfigurationException::class)
            .exceptionMessage
            .shouldStartWith("Following subscribers are invalid [testBean#onMessage]")
    }

    @Test
    fun `throws if multiple subscribers have more than one payload parameter`() {
        val bean = object {
            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "a", qos = EXACTLY_ONCE)
            fun first(a: String, b: String) = Unit

            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "b", qos = EXACTLY_ONCE)
            fun second(a: String, b: String) = Unit
        }

        invoking { annotationCollector.postProcessAfterInitialization(bean, "testBean") }
            .shouldThrow(MqttConfigurationException::class)
            .exceptionMessage
            .shouldStartWith("Following subscribers are invalid [testBean#first, testBean#second].")
    }
}
