package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import com.hivemq.client.mqtt.datatypes.MqttTopic
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldNotThrow
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.jupiter.api.Test

internal class AnnotationCollectorTests {

    private val annotationCollector = AnnotationCollector()

    @Test
    internal fun `passes if only a parameter is defined`() {
        val bean = object {
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage(a: String) {
            }
        }

        invoking { annotationCollector.postProcessBeforeInitialization(bean, "testBean") }
            .shouldNotThrow(MqttConfigurationException::class)
    }

    @Test
    internal fun `passes if parameter and topic is defined`() {
        val bean = object {
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage(a: String, topic: MqttTopic) {
            }
        }

        invoking { annotationCollector.postProcessBeforeInitialization(bean, "testBean") }
            .shouldNotThrow(MqttConfigurationException::class)
    }

    @Test
    internal fun `throws if one subscriber has more than one parameter`() {
        val bean = object {
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage(a: String, b: String) {
            }
        }

        invoking { annotationCollector.postProcessBeforeInitialization(bean, "testBean") }
            .shouldThrow(MqttConfigurationException::class)
            .withMessage("Subscriber onMessage should have exactly one parameter.")
    }

    @Test
    internal fun `throws if multiple subscribers have more than one parameter`() {
        val bean = object {
            @MqttSubscribe(topic = "a", qos = EXACTLY_ONCE)
            fun first(a: String, b: String) {
            }

            @MqttSubscribe(topic = "b", qos = EXACTLY_ONCE)
            fun second(a: String, b: String) {
            }
        }

        invoking { annotationCollector.postProcessBeforeInitialization(bean, "testBean") }
            .shouldThrow(MqttConfigurationException::class)
            .withMessage("Subscriber [first, second] should have exactly one parameter.")
    }

}
