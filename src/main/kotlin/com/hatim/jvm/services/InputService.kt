package com.hatim.jvm.services

import net.openhft.chronicle.core.Jvm
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PreDestroy

@Service
class InputService : ApplicationRunner {
    companion object {
        private val logger = LoggerFactory.getLogger(InputService::class.java)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val continueReading = AtomicBoolean(true)

    override fun run(args: ApplicationArguments?) {
        executor.execute(this::startReader)
    }

    @PreDestroy
    fun destroy() {
        continueReading.set(false)
    }

    private fun startReader() {
        SingleChronicleQueueBuilder.single("D:/queues")
                .build().use { queue ->
                    queue.createTailer().apply {
                        while (continueReading.get()) {
                            val message: String? = readText()
                            if (message == null) {
                                Jvm.pause(1)
                            } else {
                                val difference = (System.nanoTime() - message.toLong()) / 1e3
                                logger.info("Took : {}", difference)
                            }
                        }
                    }
                }
    }
}