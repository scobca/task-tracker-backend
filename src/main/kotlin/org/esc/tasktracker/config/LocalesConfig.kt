package org.esc.tasktracker.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

@Configuration
open class LocalesConfig {

    @Bean
    open fun messageSource(): MessageSource {
        val source = ResourceBundleMessageSource()

        source.setBasenames(
            "i18n/errors",
            "i18n/responses"
        )
        source.setDefaultEncoding("UTF-8")
        return source
    }
}