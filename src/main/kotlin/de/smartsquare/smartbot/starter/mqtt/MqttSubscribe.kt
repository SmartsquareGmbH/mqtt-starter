package de.smartsquare.smartbot.starter.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class MqttSubscribe(

  val topic: String,

  val qos: MqttQos,

  val shared: Boolean = false,
)
