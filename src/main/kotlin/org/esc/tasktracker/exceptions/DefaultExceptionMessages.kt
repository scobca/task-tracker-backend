package org.esc.tasktracker.exceptions

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

    fun exceptionOccurred() = messageSource.getMessage(
        "error.exception_occurred",
        null,
        Locale.of(defaultLocale)
    )

    fun badValidation() = messageSource.getMessage(
        "error.bad_validation",
        null,
        Locale.of(defaultLocale)
    )

    fun userNotFound() = messageSource.getMessage(
        "error.user.not_found",
        null,
        Locale.of(defaultLocale)
    )

    fun userDoubleRecord() = messageSource.getMessage(
        "error.user.double_record",
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

    fun wrongUsernameOrPassword() = messageSource.getMessage(
        "error.wrong_username_or_password",
        null,
        Locale.of(defaultLocale)
    )

    fun wrongRefreshToken() = messageSource.getMessage(
        "error.wrong_refresh_token",
        null,
        Locale.of(defaultLocale)
    )

    fun badJWTVerification() = messageSource.getMessage(
        "error.bad_jwt_verification",
        null,
        Locale.of(defaultLocale)
    )

    fun accessDenied() = messageSource.getMessage(
        "error.access_denied",
        null,
        Locale.of(defaultLocale)
    )
}