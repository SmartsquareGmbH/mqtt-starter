package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import de.smartsquare.starter.mqtt.mapper.JacksonMqttObjectMapper
import de.smartsquare.starter.mqtt.mapper.MqttObjectMapper
import org.amshove.kluent.AnyException
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.boot.context.annotation.UserConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import tools.jackson.databind.json.JsonMapper

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

    @Test
    fun `should allow configuring custom mqtt object mapper`() {
        runner.withBean(MqttObjectMapper::class.java, { CustomMqttObjectMapper() })
            .run { context ->
                val mapper = context.getBean<MqttObjectMapper>()

                mapper.fromBytes("test".toByteArray(), String::class.java) shouldBeEqualTo "fromBytes"
                mapper.toBytes("test") shouldBeEqualTo "toBytes"
            }
    }

    class CustomMqttObjectMapper : MqttObjectMapper {
        override fun fromBytes(bytes: ByteArray, targetType: Class<*>) = "fromBytes"
        override fun toBytes(value: Any) = "toBytes"
    }

    @Test
    fun `should fallback to error mqtt object mapper when no library is configured`() {
        runner.run { context ->
            val mapper = context.getBean<MqttObjectMapper>()

            invoking { mapper.fromBytes("test".toByteArray(), String::class.java) } shouldThrow AnyException
            invoking { mapper.toBytes("test") } shouldThrow AnyException
        }
    }

    @Test
    fun `should use jackson when available`() {
        runner
            .withBean(JsonMapper::class.java, { JsonMapper() })
            .run { context ->
                val mapper = context.getBean<MqttObjectMapper>()

                mapper.shouldBeInstanceOf<JacksonMqttObjectMapper>()
            }
    }

    @Test
    fun `should work when multiple libraries are configured`() {
        runner
            .withBean(JsonMapper::class.java, { JsonMapper() })
            .withBean(ObjectMapper::class.java, { ObjectMapper() })
            .withBean(Gson::class.java, { Gson() })
            .run { context ->
                val mapper = context.getBean<MqttObjectMapper>()

                mapper.shouldBeInstanceOf<JacksonMqttObjectMapper>()
            }
    }
}
