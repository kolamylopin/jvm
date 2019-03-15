package com.hatim.jvm.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
final data class Configuration(
        @Value("\${application.wait-in-microseconds-between-reads}") val waitInMicroSecBetweenReads: Long,
        @Value("\${application.engine.name}") val engineName: String,
        @Value("\${application.engine.connections-attempts}") val engineConnectionsAttempts: Int,
        @Value("\${application.input-queue}") val inputQueue: String) {
    val waitInNanoBetweenReads = 1000 * waitInMicroSecBetweenReads
}
