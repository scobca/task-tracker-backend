package org.esc.tasktracker

import jakarta.annotation.PostConstruct
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
open class EscTaskTrackerApplication(private val localeTestClass: LocaleTestClass) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<EscTaskTrackerApplication>(*args)
        }
    }

    @PostConstruct
    fun init() {
        localeTestClass.logMessage()
        localeTestClass.logError()
    }
}