package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.JsonMappingException
import com.hivemq.client.mqtt.datatypes.MqttTopic
import de.smartsquare.starter.mqtt.MqttSubscriberCollector.ResolvedMqttSubscriber
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap

/**
 * Class for consuming and forwarding messages to the correct subscriber.
 */
class MqttHandler(
    private val collector: MqttSubscriberCollector,
    private val adapter: MqttMessageAdapter,
    private val messageErrorHandler: MqttMessageErrorHandler,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val subscriberCache = ConcurrentHashMap<MqttTopic, ResolvedMqttSubscriber>(collector.subscribers.size)

    /**
     * Handles a single [message]. The topic of the message is used to determine the correct subscriber which is then
     * invoked with parameters produced by the [MqttMessageAdapter].
     */
    fun handle(message: MqttPublishContainer) {
        val topic = message.topic

        if (logger.isTraceEnabled) {
            logger.trace("Received mqtt message on topic [$topic] with payload ${message.payload}")
        }

        val subscriber = subscriberCache
            .getOrPut(topic) { collector.subscribers.find { it.topic.matches(topic) } }
            ?: error("No subscriber found for topic $topic")

        try {
            val parameters = subscriber.method.parameterTypes
                .map { adapter.adapt(message, it) }
                .toTypedArray()

            subscriber.method.invoke(subscriber.bean, *parameters)
        } catch (e: InvocationTargetException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    topic,
                    message.payload,
                    "Error while handling mqtt message on topic [$topic]",
                    e,
                ),
            )
        } catch (e: JsonMappingException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    topic,
                    message.payload,
                    "Error while handling mqtt message on topic [$topic]: Failed to map payload to target class",
                    e,
                ),
            )
        } catch (e: JacksonException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    topic,
                    message.payload,
                    "Error while handling mqtt message on topic [$topic]: Failed to parse payload",
                    e,
                ),
            )
        }
    }
}
