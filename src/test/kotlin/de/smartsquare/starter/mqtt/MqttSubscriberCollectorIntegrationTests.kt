package de.smartsquare.starter.mqtt

import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
        fun annotationCollector() = MqttSubscriberCollector(TestObjectProvider(MqttProperties()))

        @Bean
        fun subscriber() = IntSubscriber()
    }
}
