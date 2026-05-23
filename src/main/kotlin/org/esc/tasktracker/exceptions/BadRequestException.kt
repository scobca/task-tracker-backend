package org.esc.tasktracker.exceptions

import org.springframework.http.HttpStatus

/**
 * Exception thrown when a request contains invalid data or parameters.
 *
 * Maps to HTTP 400 Bad Request status code.
 * Used for validation failures, malformed requests, or missing required fields.
 *
 * @param message Detailed description of what was invalid in the request.
 *
 * @see AbstractHttpException
 * @see org.esc.tasktracker.io.GlobalExceptionsHandler
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
class BadRequestException private constructor(
    status: Int,
    message: String
) : AbstractHttpException(status, message) {
    constructor(message: String) : this(HttpStatus.BAD_REQUEST.value(), message)
}