package de.smartsquare.starter.mqtt

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.JsonMappingException
import com.hivemq.client.mqtt.datatypes.MqttTopic
import de.smartsquare.starter.mqtt.MqttHandler.AnnotatedMethodDelegate
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

/**
 * Class for consuming and forwarding messages to the correct subscriber.
 */
class MqttHandler(
    private val collector: MqttSubscriberCollector,
    private val adapter: MqttMessageAdapter,
    private val messageErrorHandler: MqttMessageErrorHandler,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val subscriberCache = ConcurrentHashMap<MqttTopic, MqttSubscriberReference>(collector.subscribers.size)

    private data class MqttSubscriberReference(
        val subscriber: AnnotatedMethodDelegate,
        val parameterTypes: List<Class<*>>,
    )

    private fun interface AnnotatedMethodDelegate {
        fun invoke(vararg args: Any)
    }

    /**
     * Handles a single [message]. The topic of the message is used to determine the correct subscriber which is then
     * invoked with parameters produced by the [MqttMessageAdapter].
     */
    fun handle(message: MqttPublishContainer) {
        val (topic, payload) = message
        if (logger.isTraceEnabled) {
            logger.trace("Received mqtt message on topic [$topic] with payload $payload")
        }
        val (subscriber, parameterTypes) = getSubscriber(topic)
        try {
            subscriber.invoke(*Array(parameterTypes.size) { adapter.adapt(message, parameterTypes[it]) })
        } catch (e: JsonMappingException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    topic,
                    payload,
                    "Error while handling mqtt message on topic [$topic]: Failed to map payload to target class",
                    e,
                ),
            )
        } catch (e: JacksonException) {
            messageErrorHandler.handle(
                MqttMessageException(
                    topic,
                    payload,
                    "Error while handling mqtt message on topic [$topic]: Failed to parse payload",
                    e,
                ),
            )
        } catch (e: Exception) {
            messageErrorHandler.handle(
                MqttMessageException(topic, payload, "Error while handling mqtt message on topic [$topic]", e),
            )
        }
    }

    /**
     * Returns the subscriber for the given [topic].
     * If no subscriber is found, an error is thrown.
     * The subscriber is cached for performance reasons.
     *
     * If the function is a suspend function, it is wrapped in a suspend call. For normal functions, a method handle is
     * created and cached.
     */
    private fun getSubscriber(topic: MqttTopic): MqttSubscriberReference = subscriberCache.getOrPut(topic) {
        val subscriber = collector.subscribers.find { it.topic.matches(topic) }
            ?: error("No subscriber found for topic $topic")
        val kFunction = subscriber.method.kotlinFunction
        val parameterTypes = kFunction?.valueParameters?.map { it.type.jvmErasure.java }
            ?: subscriber.method.parameterTypes.toList()
        val delegate = if (kFunction?.isSuspend == true) {
            AnnotatedMethodDelegate { args -> runBlocking { kFunction.callSuspend(subscriber.bean, *args) } }
        } else {
            val handle = MethodHandles.publicLookup().unreflect(subscriber.method)
            AnnotatedMethodDelegate { args -> handle.invokeWithArguments(subscriber.bean, *args) }
        }

        MqttSubscriberReference(delegate, parameterTypes)
    }
}
