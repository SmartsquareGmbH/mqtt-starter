package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import org.springframework.beans.factory.DisposableBean
import java.util.concurrent.TimeUnit

class DisposableMqtt3Client(private val delegate: Mqtt3Client) : Mqtt3Client by delegate, DisposableBean {

    override fun destroy() {
        delegate.toAsync().disconnect().get(10, TimeUnit.SECONDS)
    }
}
