package de.smartsquare.starter.mqtt

import org.springframework.context.event.ContextClosedEvent
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

class MqttExecutor : ThreadPoolTaskExecutor() {

    init {
        corePoolSize = Runtime.getRuntime().availableProcessors()
        phase = MqttConnector.SMART_LIFECYCLE_PHASE - 1024
        threadNamePrefix = "mqtt-executor-"

        setWaitForTasksToCompleteOnShutdown(true)
    }

    override fun onApplicationEvent(event: ContextClosedEvent) {
        // Ignore lateShutdown logic since it causes our executor to not be part of the normal shutdown order.
    }
}
