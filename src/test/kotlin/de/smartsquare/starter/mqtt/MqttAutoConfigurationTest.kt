package de.smartsquare.starter.mqtt

import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.annotation.UserConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class MqttAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(UserConfigurations.of(MqttAutoConfiguration::class.java))

    @Test
    fun `should not be loaded if mqtt enabled is set to false`() {
        runner.withPropertyValues("mqtt.enabled=false")
            .run { context ->
                invoking { context.getBean<MqttAutoConfiguration>() } shouldThrow
                    NoSuchBeanDefinitionException::class withMessage
                    "No qualifying bean of type 'de.smartsquare.starter.mqtt.MqttAutoConfiguration' available"
            }
    }
}
