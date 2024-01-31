package de.smartsquare.starter.mqtt

import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.scheduling.concurrent.CustomizableThreadFactory
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

/**
 * [Executor] subclass implementing [SmartLifecycle] to wait for active tasks during [stop].
 *
 * This delegates to a [ThreadPoolExecutor] with the number of processors as max threads.
 */
class MqttGracefulExecutor : Executor, SmartLifecycle {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val delegate = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        CustomizableThreadFactory("mqtt-executor-"),
    ) as ThreadPoolExecutor

    override fun execute(task: Runnable) {
        delegate.execute(task)
    }

    override fun start() = Unit

    override fun stop() {
        if (delegate.activeCount > 0) {
            logger.info("Commencing graceful shutdown. Waiting for active tasks to complete")

            // Graceful shutdown: Periodically check if the delegate has finished all tasks and wait 10ms if not yet.
            while (delegate.activeCount > 0) {
                Thread.sleep(10)
            }

            logger.info("Graceful shutdown complete")
        }
    }

    override fun isRunning(): Boolean {
        return !delegate.isShutdown
    }

    override fun getPhase(): Int {
        return MqttConnector.SMART_LIFECYCLE_PHASE - 512
    }
}
