package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@SpringBootTest(classes = [AnnotationCollectorIntegrationTests.PostProcessorConfiguration::class, AnnotationCollectorIntegrationTests.JacksonConfiguration::class])
class AnnotationCollectorIntegrationTests {

    @Autowired
    private lateinit var annotationCollector: AnnotationCollector

    @Test
    fun `find subscriber bean`() {
        annotationCollector.subscribers.shouldHaveSize(1)
    }

    @Configuration
    class JacksonConfiguration {

        @Bean
        fun jackson(): ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
    }

    @Configuration
    class PostProcessorConfiguration {

        @Bean
        fun annotationCollector() = AnnotationCollector()

        @Bean
        fun subscriber() = Subscriber()
    }

    data class TemperatureMessage(val value: Int)

    @Component
    class Subscriber {

        @MqttSubscribe(topic = "topic", qos = EXACTLY_ONCE)
        fun onMessage(payload: TemperatureMessage) {
        }
    }
}
