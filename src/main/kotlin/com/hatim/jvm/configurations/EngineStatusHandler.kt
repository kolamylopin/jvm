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

@Component
class EngineStatusHandler(@Autowired val discoveryClient: EurekaClient)
    : EurekaEventListener, ApplicationRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(EngineStatusHandler::class.java)
    }

    @Autowired
    private val context: ApplicationContext? = null

    override fun onEvent(event: EurekaEvent?) {
        if (discoveryClient.getApplication("engine") == null) {
            logger.error("No engine is available! Shutting down")
            if (context is ConfigurableApplicationContext) {
                context.close()
            }
        }
    }

    override fun run(args: ApplicationArguments?) {
        discoveryClient.registerEventListener(this)
    }
}