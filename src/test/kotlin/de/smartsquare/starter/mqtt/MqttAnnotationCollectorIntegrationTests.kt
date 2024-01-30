package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS

@DirtiesContext(classMode = BEFORE_CLASS)
@SpringBootTest(classes = [MqttAnnotationCollectorIntegrationTests.PostProcessorConfiguration::class])
class MqttAnnotationCollectorIntegrationTests {

    @Autowired
    private lateinit var annotationCollector: MqttAnnotationCollector

    @Test
    fun `find subscriber bean`() {
        annotationCollector.subscribers.shouldHaveSize(1)
    }

    @Configuration
    class PostProcessorConfiguration {

        @Bean
        fun annotationCollector() = MqttAnnotationCollector()

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
