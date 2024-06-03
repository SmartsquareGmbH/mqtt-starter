package de.smartsquare.starter.mqtt

import de.smartsquare.starter.mqtt.health.MqttHealthAutoConfiguration
import de.smartsquare.starter.mqtt.health.MqttHealthIndicator
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.net.URLClassLoader

@ExtendWith(EmqxExtension::class)
class MqttHealthAutoConfigurationTest {

    private val runner = ApplicationContextRunner().withConfiguration(
        AutoConfigurations.of(MqttAutoConfiguration::class.java, MqttHealthAutoConfiguration::class.java),
    )

    @Test
    fun `no health indicator if mqtt auto starter is disabled`() {
        runner.withPropertyValues("mqtt.enabled=false")
            .run { context ->
                invoking { context.getBean<MqttHealthIndicator>() } shouldThrow
                    NoSuchBeanDefinitionException::class withMessage
                    "No qualifying bean of type 'de.smartsquare.starter.mqtt.health.MqttHealthIndicator' available"
            }
    }

    @Test
    fun `no health indicator if mqtt health component gets disabled`() {
        runner.withPropertyValues("management.health.mqtt.enabled=false")
            .run { context ->
                invoking { context.getBean<MqttHealthIndicator>() } shouldThrow
                    NoSuchBeanDefinitionException::class withMessage
                    "No qualifying bean of type 'de.smartsquare.starter.mqtt.health.MqttHealthIndicator' available"
            }
    }

    @Test
    fun `test auto-configuration without actuator`() {
        // exclude HealthEndpoint class to simulate missing actuator dependency
        val loader = object : URLClassLoader(arrayOf(), ClassLoader.getSystemClassLoader()) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (HealthEndpoint::class.java.name == name) {
                    throw ClassNotFoundException("Class $name is excluded")
                }
                return super.loadClass(name, resolve)
            }
        }

        runner.withClassLoader(loader).run { context ->
            invoking { context.getBean<MqttHealthIndicator>() } shouldThrow
                NoSuchBeanDefinitionException::class withMessage
                "No qualifying bean of type 'de.smartsquare.starter.mqtt.health.MqttHealthIndicator' available"
        }
    }

    @Test
    fun `should be healthy on connect`() {
        runner.withPropertyValues("mqtt.version=5").run { context ->
            val indicator = context.getBean<MqttHealthIndicator>()

            indicator.health().status.`should be equal to`(Status.UP)
        }
    }

    @Test
    fun `should be unhealthy on connect`() {
        // Replace MqttConnector to prevent connection on startup for broker to stay in "DISCONNECTED" state
        class MqttTestConnectorConfiguration : MqttConnector() {
            override fun stop(callback: Runnable) = callback.run()
            override fun start() = Unit
            override fun isRunning(): Boolean = false
        }

        runner.withPropertyValues("mqtt.version=5")
            .withBean("mqtt5Connector", MqttTestConnectorConfiguration::class.java)
            .withAllowBeanDefinitionOverriding(true)
            .run { context ->
                val indicator = context.getBean<MqttHealthIndicator>()

                indicator.health().status.`should be equal to`(Status.DOWN)
            }
    }
}
