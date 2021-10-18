package de.smartsquare.starter.mqtt

import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test
import org.springframework.boot.context.annotation.UserConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class MqttAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(UserConfigurations.of(MqttAutoConfiguration::class.java))

    @Test
    fun `should not be loaded if mqtt enabled is set to false`() {
        runner.withPropertyValues("mqtt.enabled=false")
            .run { context -> context.containsBean("mqttAutoConfiguration").shouldBeFalse() }
    }
}
