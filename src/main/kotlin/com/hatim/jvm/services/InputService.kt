package com.hatim.jvm.services

import com.hatim.jvm.data.PricingRequest
import com.hatim.jvm.utils.Configuration
import net.openhft.chronicle.queue.ExcerptTailer
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import net.openhft.chronicle.wire.ReadMarshallable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cloud.netflix.eureka.CloudEurekaInstanceConfig
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import javax.annotation.PreDestroy

@Service
class InputService(@Autowired private val instanceConfig: CloudEurekaInstanceConfig,
                   @Autowired private val configuration: Configuration) : ApplicationRunner {
    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(InputService::class.java)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val shuttingDown = AtomicBoolean(false)

    override fun run(args: ApplicationArguments?) {
        executor.execute(this::startReader)
    }

    @PreDestroy
    fun destroy() {
        shuttingDown.set(true)
        executor.shutdown()
    }

    private fun startReader() {
        SingleChronicleQueueBuilder.single(configuration.inputQueue)
                .build().use { queue ->
                    queue.createTailer().apply {
                        toEnd()
                        val requestCreator = { PricingRequest() }
                        while (!shuttingDown.get()) {
                            instanceConfig.instanceId.let { destination ->
                                val request = lazilyReadDocument(requestCreator)
                                if (request == null) {
//                                        Jvm.pause(1)
                                    LockSupport.parkNanos(configuration.waitInNanoBetweenReads)
                                } else {
                                    if (request is PricingRequest) {
                                        if (destination == request.destination) {
                                            notifyListeners(request)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
    }

    private fun notifyListeners(pricingRequest: PricingRequest) {
        logger.info("Processing $pricingRequest")
    }
}

fun ExcerptTailer.lazilyReadDocument(readerCreator: () -> ReadMarshallable): ReadMarshallable? {
    readingDocument().use { dc ->
        if (!dc.isPresent) {
            return null
        }
        readerCreator().run {
            readMarshallable(dc.wire()!!)
            return this
        }
    }
}
