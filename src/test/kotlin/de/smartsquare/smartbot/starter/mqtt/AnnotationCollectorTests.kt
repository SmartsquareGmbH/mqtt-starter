package de.smartsquare.smartbot.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hivemq.client.mqtt.datatypes.MqttQos.EXACTLY_ONCE
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@SpringBootTest(classes = [AnnotationCollectorTests.PostProcessorConfiguration::class, AnnotationCollectorTests.JacksonConfiguration::class])
internal class AnnotationCollectorTests {

    @Autowired
    private lateinit var annotationCollector: AnnotationCollector

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var jackson: ObjectMapper

    @Test
    internal fun `find subscriber bean`() {
        annotationCollector.subscribers.shouldHaveSize(1)

        for ((bean, subscribers) in annotationCollector.subscribers) {
            for (subscriber in subscribers) {
                val resolvedParameters = subscriber.parameterTypes.map { getBean(it) ?: jackson.readValue("""{"value": 1}""", it) }

                subscriber.invoke(bean, *resolvedParameters.toTypedArray())
            }
        }
    }

    private fun getBean(parameter: Class<*>): Any? {
        return try {
            context.getBean(parameter)
        } catch (e: NoSuchBeanDefinitionException) {
            null
        }
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
        fun onMessage(payload: TemperatureMessage, anySpringBean: AnnotationCollector) {
            println("Invoked :) [Temp: ${payload.value}, Collector: ${anySpringBean}]")
        }
    }
}
