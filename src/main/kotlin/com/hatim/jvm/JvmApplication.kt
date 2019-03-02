package com.hatim.jvm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@SpringBootApplication
@EnableDiscoveryClient
class JvmApplication {
    @Bean
    fun getExecutor(): Executor = Executors.newSingleThreadExecutor()
}

fun main(args: Array<String>) {
	runApplication<JvmApplication>(*args)
}
