package com.hatim.jvm.services

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue
import com.hatim.jvm.data.PricingResponse
import com.hatim.jvm.utils.Configuration
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

@Service
class PublisherService(@Autowired private val configuration: Configuration) {
    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(PublisherService::class.java)
    }

    private val messagesQueue: BlockingQueue<PricingResponse> =
            DisruptorBlockingQueue<PricingResponse>(configuration.messagesQueueSize)

    private val queueAppender = SingleChronicleQueueBuilder
            .single(configuration.outputQueue).build()
            .acquireAppender()

    private val executorService = Executors.newSingleThreadExecutor()

    init {
        publishQueueMessages()
    }

    @PreDestroy
    fun destroy() {
        executorService.shutdownNow()
    }

    private fun publishQueueMessages() {
        executorService.execute {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val message = messagesQueue.take()
                    queueAppender.writeDocument(message)
                    logger.info("published $message")
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    fun addToQueue(message: PricingResponse) = messagesQueue.add(message)
}
