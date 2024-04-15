package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class MqttSubscriberCollectorTests {

    private val annotationCollector = MqttSubscriberCollector(TestObjectProvider(MqttProperties(group = "group")))

    @Test
    fun `processes bean`() {
        val bean = object {
            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE)
            fun onMessage(payload: String) = Unit
        }

        annotationCollector.postProcessAfterInitialization(bean, "testBean")

        annotationCollector.subscribers.shouldHaveSize(1)
        annotationCollector.subscribers[0].bean shouldBe bean
        annotationCollector.subscribers[0].method.name shouldBeEqualTo "onMessage"
        annotationCollector.subscribers[0].qos shouldBeEqualTo EXACTLY_ONCE
        annotationCollector.subscribers[0].topic.toString() shouldBeEqualTo "test"
    }

    @Test
    fun `processes bean with share enabled`() {
        val bean = object {
            @Suppress("unused", "UNUSED_PARAMETER")
            @MqttSubscribe(topic = "test", qos = EXACTLY_ONCE, shared = true)
            fun onMessage(payload: String) = Unit
        }

        annotationCollector.postProcessAfterInitialization(bean, "testBean")

        annotationCollector.subscribers.shouldHaveSize(1)
        annotationCollector.subscribers[0].bean shouldBe bean
        annotationCollector.subscribers[0].method.name shouldBeEqualTo "onMessage"
        annotationCollector.subscribers[0].qos shouldBeEqualTo EXACTLY_ONCE
        annotationCollector.subscribers[0].topic.toString() shouldBeEqualTo "\$share/group/test"
    }
}
