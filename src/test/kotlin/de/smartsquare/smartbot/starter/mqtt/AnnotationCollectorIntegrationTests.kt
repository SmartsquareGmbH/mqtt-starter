package de.smartsquare.smartbot.starter.mqtt

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
internal class AnnotationCollectorIntegrationTests {

    @Autowired
    private lateinit var annotationCollector: AnnotationCollector

    @Test
    internal fun `find subscriber bean`() {
        annotationCollector.subscribers.shouldHaveSize(1)
    }

    @Configuration
    open class JacksonConfiguration {

        @Bean
        open fun jackson(): ObjectMapper = jacksonObjectMapper().findAndRegisterModules()
    }

    @Configuration
    open class PostProcessorConfiguration {

        @Bean
        open fun annotationCollector() = AnnotationCollector()

        @Bean
        open fun subscriber() = Subscriber()
    }

    data class TemperatureMessage(val value: Int)

    @Component
    class Subscriber {

        @MqttSubscribe(topic = "topic", qos = EXACTLY_ONCE)
        fun onMessage(payload: TemperatureMessage) {
        }
    }
}
