package com.hatim.jvm.controllers

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RestController {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    @RequestMapping("/")
    fun getDefaultMessage() = getMessage("Default")

    @RequestMapping("/message/{message}")
    fun getMessage(@PathVariable("message") message: String): String {
        logger.info("Received request for message $message")
        return "Returned $message"
    }
}