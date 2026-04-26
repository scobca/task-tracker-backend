package org.esc.tasktracker

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EscTaskTrackerApplication(private val localeTestClass: LocaleTestClass) {

    @PostConstruct
    fun init() {
        localeTestClass.logMessage()
        localeTestClass.logError()
    }
}

fun main(args: Array<String>) {
    runApplication<EscTaskTrackerApplication>(*args)
}
