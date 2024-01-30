package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@SpringBootTest(classes = [MqttSubscriberCollectorIntegrationTests.PostProcessorConfiguration::class])
class MqttSubscriberCollectorIntegrationTests {

    @Autowired
    private lateinit var annotationCollector: MqttSubscriberCollector

    @Test
    fun `find subscriber bean`() {
        annotationCollector.subscribers.shouldHaveSize(1)
    }

    @Configuration
    class PostProcessorConfiguration {

        @Bean
        fun annotationCollector() = MqttSubscriberCollector(MqttProperties())

        @Bean
        fun subscriber() = Subscriber()
    }

    @Component
    class Subscriber {

        @Suppress("unused", "UNUSED_PARAMETER")
        @MqttSubscribe(topic = "topic", qos = EXACTLY_ONCE)
        fun onMessage(payload: TemperatureMessage) = Unit
    }
}
