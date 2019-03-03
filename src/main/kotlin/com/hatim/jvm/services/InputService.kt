package com.hatim.jvm.services

import net.openhft.chronicle.core.Jvm
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cloud.netflix.eureka.CloudEurekaInstanceConfig
import org.springframework.stereotype.Service
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PreDestroy

@Service
class InputService(private val executor: Executor) : ApplicationRunner {
    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(InputService::class.java)
    }

    @Autowired
    private var instanceConfig: CloudEurekaInstanceConfig? = null
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
                                instanceConfig?.instanceId?.let {
                                    val parts = message.split("$")
                                    if (parts.size >= 2 && it == parts[0]) {
                                        logger.info("Received : {}", parts[1])
                                    }
                                }
                            }
                        }
                    }
                }
    }
}