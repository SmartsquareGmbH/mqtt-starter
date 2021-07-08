package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SpringAwareMqtt3Client(
    private val delegate: Mqtt3Client,
    private val connect: Mqtt3Connect
) : Mqtt3Client by delegate, InitializingBean, DisposableBean {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun afterPropertiesSet() {
        val host = delegate.config.serverHost
        val port = delegate.config.serverPort
        val username = delegate.config.simpleAuth.orElseGet { null }?.username?.toString()

        logger.info("Connecting to ${if (username != null) "$username@" else ""}$host:$port...")

        try {
            val acknowledgement = delegate.toAsync()
                .connect(connect)
                .get(10, TimeUnit.SECONDS)

            if (acknowledgement.returnCode.isError) {
                throw BrokerConnectException(acknowledgement)
            }
        } catch (error: TimeoutException) {
            throw BrokerConnectException(
                "Failed to connect: Broker $host:$port did not respond within 10 seconds.",
                error
            )
        }
    }

    override fun destroy() {
        delegate.toAsync().disconnect().get(10, TimeUnit.SECONDS)
    }
}
