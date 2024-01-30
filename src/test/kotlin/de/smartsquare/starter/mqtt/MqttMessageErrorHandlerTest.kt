package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.core.JsonProcessingException
import com.hivemq.client.mqtt.datatypes.MqttQos
import de.smartsquare.starter.mqtt.MqttMessageErrorHandlerTest.CustomErrorHandler
import de.smartsquare.starter.mqtt.MqttMessageErrorHandlerTest.IntSubscriber
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExtendWith(EmqxExtension::class)
@SpringBootTest(
    classes = [
        MqttAutoConfiguration::class,
        CustomErrorHandler::class,
        IntSubscriber::class,
    ],
)
class MqttMessageErrorHandlerTest {

    @Autowired
    private lateinit var publisher: Mqtt3Publisher

    @Autowired
    private lateinit var errorHandler: CustomErrorHandler

    @Test
    fun `should call error handler when receiving invalid messages`() {
        publisher.publish("int", MqttQos.EXACTLY_ONCE, "error")

        errorHandler.await().shouldBeTrue()
    }

    @Component
    class CustomErrorHandler : MqttMessageErrorHandler() {

        private val countDownLatch = CountDownLatch(1)

        override fun handle(error: MqttMessageException) {
            error.topic.toString() shouldBeEqualTo "int"
            error.payload.decodeToString() shouldBeEqualTo "error"
            error.cause.shouldBeInstanceOf<JsonProcessingException>()

            countDownLatch.countDown()
        }

        fun await() = countDownLatch.await(5, TimeUnit.SECONDS)
    }

    @Component
    class IntSubscriber {

        @Suppress("UnusedParameter")
        @MqttSubscribe(topic = "int", qos = MqttQos.EXACTLY_ONCE)
        fun onMessage(payload: Int) = Unit
    }
}
