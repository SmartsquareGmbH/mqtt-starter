package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory

class MqttSubscriberRegistryTests {

    private val beanFactory = DefaultListableBeanFactory()
    private val registry = MqttSubscriberRegistry(beanFactory, MqttProperties(group = "group"))

    @Test
    fun `processes bean`() {
        val bean = IntSubscriber()

        beanFactory.registerSingleton("testBean", bean)
        registry.afterSingletonsInstantiated()

        registry.subscribers.shouldHaveSize(1)
        registry.subscribers[0].qos shouldBeEqualTo EXACTLY_ONCE
        registry.subscribers[0].topic.toString() shouldBeEqualTo "int"
    }

    @Test
    fun `processes bean with share enabled`() {
        val bean = ShareSubscriber()

        beanFactory.registerSingleton("testBean", bean)
        registry.afterSingletonsInstantiated()

        registry.subscribers.shouldHaveSize(1)
        registry.subscribers[0].qos shouldBeEqualTo EXACTLY_ONCE
        registry.subscribers[0].topic.toString() shouldBeEqualTo $$"$share/group/shared"
    }
}
