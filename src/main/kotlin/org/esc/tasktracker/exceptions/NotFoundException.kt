package org.esc.tasktracker.exceptions

import org.springframework.http.HttpStatus

/**
 * Exception thrown when a requested resource cannot be found.
 *
 * Maps to HTTP 404 Not Found status code.
 * Used when entities are queried by ID or email but don't exist in the database.
 *
 * @param message Description of what resource was not found.
 *
 * @see AbstractHttpException
 * @see org.esc.tasktracker.io.GlobalExceptionsHandler
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
class NotFoundException private constructor(
    status: Int,
    message: String
) : AbstractHttpException(status, message) {
    constructor(message: String) : this(HttpStatus.NOT_FOUND.value(), message)
}