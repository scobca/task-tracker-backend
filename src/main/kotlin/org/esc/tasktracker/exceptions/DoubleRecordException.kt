package org.esc.tasktracker.exceptions

import org.springframework.http.HttpStatus

/**
 * Exception thrown when attempting to create a duplicate record.
 *
 * Maps to HTTP 409 Conflict status code.
 * Used when a unique constraint violation occurs (e.g., duplicate email, username).
 *
 * @param message Description of the conflict. Defaults to generic message if not provided.
 *
 * @see AbstractHttpException
 * @see org.esc.tasktracker.io.GlobalExceptionsHandler
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
class DoubleRecordException private constructor(
    status: Int,
    message: String?
) : AbstractHttpException(status, message) {
    constructor(message: String) : this(HttpStatus.CONFLICT.value(), message)
}