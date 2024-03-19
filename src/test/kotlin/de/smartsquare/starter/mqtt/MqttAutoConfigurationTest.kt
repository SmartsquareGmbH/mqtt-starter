package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.annotation.UserConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

@ExtendWith(EmqxExtension::class)
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

    @Test
    fun `should shutdown cleanly`() {
        runner.withPropertyValues("mqtt.version=5").run { context ->
            val client = context.getBean<Mqtt5Client>()

            client.state.isConnected.shouldBeTrue()

            context.stop()

            client.state.isConnectedOrReconnect.shouldBeFalse()
        }
    }
}
