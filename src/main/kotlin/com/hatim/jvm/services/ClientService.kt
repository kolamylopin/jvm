package com.hatim.jvm.services

import com.hatim.jvm.data.PricingRequest
import com.hatim.jvm.data.PricingResponse
import com.hatim.jvm.utils.Configuration
import net.openhft.chronicle.core.Jvm
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
import javax.annotation.PreDestroy

@Service
class ClientService(@Autowired private val instanceConfig: CloudEurekaInstanceConfig,
                    @Autowired private val configuration: Configuration,
                    @Autowired private val publisherService: PublisherService) : ApplicationRunner {
    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(ClientService::class.java)
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
                        // TODO: maybe use a ring buffer to reuse the created objects
                        val requestCreator = { PricingRequest() }
                        while (!shuttingDown.get()) {
                            instanceConfig.instanceId.let { destination ->
                                val request = lazilyReadDocument(requestCreator)
                                if (request == null) {
                                    Jvm.pause(1)
//                                    LockSupport.parkNanos(configuration.waitInNanoBetweenReads)
                                } else {
                                    if (request is PricingRequest) {
                                        if (destination == request.destination) {
                                            processingPricingRequest(request)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
    }

    private fun processingPricingRequest(request: PricingRequest) {
        val delay = (System.nanoTime() - request.timestamp) / 1e6
        logger.info("Received request ${request.id} within $delay ms")
        val response = PricingResponse(request.id, "Result", System.nanoTime(), request.timestamp)
        publisherService.addToQueue(response)
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
