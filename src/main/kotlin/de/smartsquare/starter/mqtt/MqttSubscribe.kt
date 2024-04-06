package de.smartsquare.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import org.springframework.aot.hint.annotation.Reflective

/**
 * Marker annotation for methods that should receive messages from the mqtt broker.
 */
@Reflective
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class MqttSubscribe(

    /**
     * The topic to subscribe on.
     */
    val topic: String,

    /**
     * The quality of service level to use.
     */
    val qos: MqttQos,

    /**
     * If the topic should be shared. Requires the `group` property to be set.
     *
     * See [shared subscriptions](https://www.hivemq.com/blog/mqtt5-essentials-part7-shared-subscriptions).
     */
    val shared: Boolean = false,
)
