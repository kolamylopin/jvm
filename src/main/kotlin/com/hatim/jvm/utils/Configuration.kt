package com.hatim.jvm.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class Configuration(
        @Value("\${application.wait-in-microseconds-between-reads}") val waitInMsBetweenReads: Long,
        @Value("\${application.engine.name}") val engineName: String,
        @Value("\${application.engine.connections-attempts}") val engineConnectionsAttempts: Int,
        @Value("\${application.input-queue}") val inputQueue: String)
