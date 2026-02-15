package de.smartsquare.starter.mqtt

import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootTest(classes = [MqttSubscriberRegistryIntegrationTests.PostProcessorConfiguration::class])
class MqttSubscriberRegistryIntegrationTests {

    @Autowired
    private lateinit var registry: MqttSubscriberRegistry

    @Test
    fun `find subscriber bean`() {
        registry.subscribers.shouldHaveSize(1)
    }

    @Configuration
    class PostProcessorConfiguration {

        @Bean
        fun registry(beanFactory: ListableBeanFactory) = MqttSubscriberRegistry(beanFactory, MqttProperties())

        @Bean
        fun subscriber() = IntSubscriber()
    }
}
