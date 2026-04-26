package org.esc.tasktracker

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class LocaleTestClass(
    private val messageSource: MessageSource,
) {

    @Value($$"${spring.locales.default}")
    private lateinit var defaultLocale: String

    fun logMessage() {
        println(messageSource.getMessage("response.ok", null, Locale.of(defaultLocale)))
    }

    fun logError() {
        println(
            messageSource.getMessage("error.not_found", null, Locale.of(defaultLocale))
        )
    }
}