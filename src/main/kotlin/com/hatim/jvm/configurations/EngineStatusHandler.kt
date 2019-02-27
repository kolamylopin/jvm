package com.hatim.jvm.configurations

import com.netflix.discovery.EurekaClient
import com.netflix.discovery.EurekaEvent
import com.netflix.discovery.EurekaEventListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class EngineStatusHandler(@Autowired val discoveryClient: EurekaClient)
    : EurekaEventListener, ApplicationRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(EngineStatusHandler::class.java)
    }

    @Autowired
    private val context: ApplicationContext? = null
    private var attempts = AtomicInteger()

    override fun onEvent(event: EurekaEvent?) {
        val engineInstances = discoveryClient.getApplication("engine")
        if (engineInstances == null) {
            if (attempts.getAndIncrement() >= 3) {
                logger.error("No engine available! Shutting down")
                if (context is ConfigurableApplicationContext) {
                    context.close()
                }
            } else {
                logger.warn("Unable to find engine service. Attempt {}", attempts.get())
            }
        } else {
            attempts.set(0)
        }
    }

    override fun run(args: ApplicationArguments?) {
        discoveryClient.registerEventListener(this)
    }
}