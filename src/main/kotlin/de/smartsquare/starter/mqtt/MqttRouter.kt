package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.JsonMappingException
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.datatypes.MqttTopic
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Abstract base class for all routers that handles the common implementation.
 */
abstract class MqttRouter(
    private val collector: MqttAnnotationCollector,
    private val adapter: MqttMessageAdapter,
    private val messageErrorHandler: MqttMessageErrorHandler,
    private val config: MqttProperties,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun start() {
        for ((bean, subscribers) in collector.subscribers) {
            for ((subscriberMethod, subscriberAnnotation) in subscribers) {
                val subscribeTopic = if (subscriberAnnotation.shared && config.group != null) {
                    "\$share/${config.group}/${subscriberAnnotation.topic}"
                } else {
                    subscriberAnnotation.topic
                }

                subscribe(subscribeTopic, subscriberAnnotation.qos) { topic, payload ->
                    deliver(subscriberMethod, bean, topic, payload)
                }
            }
        }
    }

    protected abstract fun subscribe(topic: String, qos: MqttQos, subscribe: (MqttTopic, ByteArray) -> Unit)

    private fun deliver(subscriber: Method, bean: Any, topic: MqttTopic, payload: ByteArray) {
        if (logger.isTraceEnabled) {
            logger.trace("Received mqtt message on topic [$topic] with payload ${payload.toString(Charsets.UTF_8)}")
        }

        try {
            val parameters = subscriber.parameterTypes.map { adapter.adapt(topic, payload, it) }.toTypedArray()

            subscriber.invoke(bean, *parameters)
        } catch (e: InvocationTargetException) {
            messageErrorHandler.handle(
                MqttMessageException(topic, payload, "Error while delivering mqtt message on topic [$topic]", e),
            )
        } catch (e: JsonMappingException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    topic,
                    payload,
                    "Error while delivering mqtt message on topic [$topic]: Failed to map payload to target class",
                    e,
                ),
            )
        } catch (e: JacksonException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    topic,
                    payload,
                    "Error while delivering mqtt message on topic [$topic]: Failed to parse payload",
                    e,
                ),
            )
        }
    }
}

/**
 * Helper class that subscribes to the mqtt broker and routes received messages to the configured subscribers (methods
 * annotated with [MqttSubscribe]).
 */
class Mqtt3Router(
    collector: MqttAnnotationCollector,
    adapter: MqttMessageAdapter,
    messageErrorHandler: MqttMessageErrorHandler,
    config: MqttProperties,
    client: Mqtt3Client,
) : MqttRouter(collector, adapter, messageErrorHandler, config) {

    private val asyncClient = client.toAsync()

    override fun subscribe(topic: String, qos: MqttQos, subscribe: (MqttTopic, ByteArray) -> Unit) {
        asyncClient.subscribeWith()
            .topicFilter(topic)
            .qos(qos)
            .callback { subscribe(it.topic, it.payloadAsBytes) }
            .send()
    }
}

/**
 * Helper class that subscribes to the mqtt broker and routes received messages to the configured subscribers (methods
 * annotated with [MqttSubscribe]).
 */
class Mqtt5Router(
    collector: MqttAnnotationCollector,
    adapter: MqttMessageAdapter,
    messageErrorHandler: MqttMessageErrorHandler,
    config: MqttProperties,
    client: Mqtt5Client,
) : MqttRouter(collector, adapter, messageErrorHandler, config) {

    private val asyncClient = client.toAsync()

    override fun subscribe(topic: String, qos: MqttQos, subscribe: (MqttTopic, ByteArray) -> Unit) {
        asyncClient.subscribeWith()
            .topicFilter(topic)
            .qos(qos)
            .callback { subscribe(it.topic, it.payloadAsBytes) }
            .send()
    }
}
