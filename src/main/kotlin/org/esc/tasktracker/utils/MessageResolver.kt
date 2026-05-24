package org.esc.tasktracker.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class MessageResolver(private val messageSource: MessageSource) {

    @Value($$"${spring.locales.default}")
    private lateinit var defaultLocale: String

    fun getMessage(key: String) = messageSource.getMessage(key, null, Locale.of(defaultLocale))
}