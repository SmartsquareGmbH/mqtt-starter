package de.smartsquare.starter.mqtt

import org.amshove.kluent.AnyException
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotThrow
import org.amshove.kluent.shouldStartWith
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.validation.BindValidationException
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.NestedExceptionUtils

class MqttPropertiesTest {

    @EnableConfigurationProperties(MqttProperties::class)
    private class TestConfiguration

    @Test
    fun `allows empty configuration`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                    ValidationAutoConfiguration::class.java
                )
            )
            .withUserConfiguration(TestConfiguration::class.java)
            .run { context: AssertableApplicationContext ->
                invoking { context.getBean(MqttProperties::class.java) } shouldNotThrow AnyException
            }
    }

    @Test
    fun `allows valid configuration`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                    ValidationAutoConfiguration::class.java
                )
            )
            .withPropertyValues(
                "mqtt.host=localhost",
                "mqtt.port=10000",
                "mqtt.client-id=clientId",
                "mqtt.username=user",
                "mqtt.password=pass",
                "mqtt.ssl=true",
                "mqtt.clean=false",
                "mqtt.group=group",
                "mqtt.version=5"
            )
            .withUserConfiguration(TestConfiguration::class.java)
            .run { context: AssertableApplicationContext ->
                val bean = context.getBean(MqttProperties::class.java)

                bean shouldBeEqualTo MqttProperties(
                    host = "localhost",
                    port = 10000,
                    clientId = "clientId",
                    username = "user",
                    password = "pass",
                    ssl = true,
                    clean = false,
                    group = "group",
                    version = 5
                )
            }
    }

    @Test
    fun `validates non empty fields`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                    ValidationAutoConfiguration::class.java
                )
            )
            .withPropertyValues("mqtt.host=")
            .withUserConfiguration(TestConfiguration::class.java)
            .run { context: AssertableApplicationContext ->
                val error = invoking {
                    context.getBean(MqttProperties::class.java)
                } shouldThrow IllegalStateException::class

                val rootError = NestedExceptionUtils.getRootCause(error.exception)
                rootError as BindValidationException

                rootError.validationErrors.allErrors.size shouldBeEqualTo 1
            }
    }

    @Test
    fun `validates port range`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                    ValidationAutoConfiguration::class.java
                )
            )
            .withPropertyValues("mqtt.port=65536")
            .withUserConfiguration(TestConfiguration::class.java)
            .run { context: AssertableApplicationContext ->
                val error = invoking {
                    context.getBean(MqttProperties::class.java)
                } shouldThrow IllegalStateException::class

                val rootError = NestedExceptionUtils.getRootCause(error.exception)
                rootError as BindValidationException

                rootError.validationErrors.allErrors.size shouldBeEqualTo 1
            }
    }

    @Test
    fun `validates mqtt version`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                    ValidationAutoConfiguration::class.java
                )
            )
            .withPropertyValues("mqtt.version=2")
            .withUserConfiguration(TestConfiguration::class.java)
            .run { context: AssertableApplicationContext ->
                val error = invoking {
                    context.getBean(MqttProperties::class.java)
                } shouldThrow IllegalStateException::class

                val rootError = NestedExceptionUtils.getRootCause(error.exception)
                rootError as BindValidationException

                rootError.validationErrors.allErrors.size shouldBeEqualTo 1
                rootError.validationErrors.allErrors[0].defaultMessage shouldStartWith "Invalid mqtt version"
            }
    }

    @Test
    fun `validates mqtt disabled`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration::class.java,
                    ValidationAutoConfiguration::class.java
                )
            )
            .withPropertyValues("mqtt.enabled=false")
            .withUserConfiguration(TestConfiguration::class.java)
            .run { context: AssertableApplicationContext ->
                invoking {
                    context.getBean(MqttAutoConfiguration::class.java)
                } shouldThrow NoSuchBeanDefinitionException::class
            }
    }
}
