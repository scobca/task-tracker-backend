package org.esc.tasktracker.enums

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class DefaultExceptionMessages(
    private val messageSource: MessageSource,
) {

    @Value($$"${spring.locales.default}")
    private lateinit var defaultLocale: String

    fun userNotFound() = messageSource.getMessage(
        "error.user.not_found",
        null,
        Locale.of(defaultLocale)
    )

    fun teamNotFound() = messageSource.getMessage(
        "error.team.not_found",
        null,
        Locale.of(defaultLocale)
    )

    fun teamMembershipNotFound() = messageSource.getMessage(
        "error.team_membership.not_found",
        null,
        Locale.of(defaultLocale)
    )

    fun teamMembershipDoubleRecord() = messageSource.getMessage(
        "error.team_membership.double_record",
        null,
        Locale.of(defaultLocale)
    )
}