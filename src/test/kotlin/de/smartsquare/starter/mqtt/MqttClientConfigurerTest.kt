package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component

@ExtendWith(EmqxExtension::class)
@SpringBootTest(
    classes = [
        MqttAutoConfiguration::class,
        MqttClientConfigurerTest.IdentifierConfigurer::class,
    ]
)
class MqttClientConfigurerTest {

    @Autowired
    private lateinit var client: Mqtt3Client

    @Test
    fun `should run configurer`() {
        client.config.clientIdentifier.get().toString() shouldBeEqualTo "test"
    }

    @Component
    class IdentifierConfigurer : MqttClientConfigurer {

        override fun configure(builder: Mqtt3ClientBuilder) {
            builder.identifier("test")
        }
    }
}
