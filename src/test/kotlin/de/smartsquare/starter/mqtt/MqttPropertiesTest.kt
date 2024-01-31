package de.smartsquare.starter.mqtt

import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@SpringBootTest(classes = [ValidationAutoConfiguration::class])
class MqttPropertiesTest {

    @Autowired
    private lateinit var validator: LocalValidatorFactoryBean

    @Test
    fun `allows minimal configuration`() {
        val errors = validator.validateObject(MqttProperties(host = "localhost", port = 10000))

        errors.allErrors.shouldBeEmpty()
    }

    @Test
    fun `allows valid configuration`() {
        val errors = validator.validateObject(
            MqttProperties(
                host = "localhost",
                port = 10000,
                clientId = "clientId",
                username = "user",
                password = "pass",
                ssl = true,
                clean = false,
                group = "group",
                version = 5,
            ),
        )

        errors.allErrors.shouldBeEmpty()
    }

    @Test
    fun `validates non empty fields`() {
        val errors = validator.validateObject(MqttProperties(host = "", port = 10000))

        errors.allErrors.shouldHaveSize(1)
    }

    @Test
    fun `validates port range`() {
        val errors = validator.validateObject(MqttProperties(host = "localhost", port = 65536))

        errors.allErrors.shouldHaveSize(1)
    }

    @Test
    fun `validates mqtt version`() {
        val errors = validator.validateObject(MqttProperties(host = "localhost", port = 10000, version = 2))

        errors.allErrors.shouldHaveSize(1)
        errors.allErrors[0].defaultMessage shouldStartWith "Invalid mqtt version"
    }

    @Test
    fun `requires host and port`() {
        val errors = validator.validateObject(MqttProperties())

        errors.allErrors.shouldHaveSize(2)

        val messages = errors.allErrors.joinToString { it.toString() }
        messages shouldContain "host"
        messages shouldContain "port"
    }
}
