package org.esc.tasktracker.io

import io.jsonwebtoken.security.SignatureException
import org.esc.tasktracker.exceptions.DefaultExceptionMessages
import org.esc.tasktracker.exceptions.AbstractHttpException
import org.esc.tasktracker.exceptions.JwtAuthenticationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Global exception handler for REST controllers that provides centralized exception handling
 * across the entire application.
 *
 * This [RestControllerAdvice] component intercepts exceptions thrown by controllers and
 * transforms them into standardized [BasicErrorResponse] objects with appropriate HTTP status
 * codes. It ensures consistent error response formatting and improves API reliability by
 * handling various exception types gracefully.
 *
 * @see RestControllerAdvice
 * @see ExceptionHandler
 * @see BasicErrorResponse
 * @see AbstractHttpException
 * @see JwtAuthenticationException
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@RestControllerAdvice
class GlobalExceptionsHandler(private val defaultExceptionMessages: DefaultExceptionMessages) {

    /**
     * Handles all unhandled exceptions that don't have specific handlers.
     *
     * This is a catch-all handler for any exception not explicitly handled by other methods.
     * It returns a 500 Internal Server Error response with the exception class name and message.
     * This ensures that even unexpected errors return a properly formatted response.
     *
     * @param ex The caught exception
     * @return ResponseEntity with HTTP 500 status and error details
     */
    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception): ResponseEntity<BasicErrorResponse> {
        val errorResponse = BasicErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = "${ex::class}; ${ex.message}",
        )

        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * Handles custom project-specific HTTP exceptions.
     *
     * Processes exceptions that extend [AbstractHttpException], which contain
     * their own HTTP status codes and messages. This allows different custom exceptions
     * to define their own response behavior while maintaining consistent formatting.
     *
     * @param ex The custom HTTP exception containing status code and message
     * @return ResponseEntity with the exception's status code and message
     */
    @ExceptionHandler(AbstractHttpException::class)
    fun handleNotFoundException(ex: AbstractHttpException): ResponseEntity<BasicErrorResponse> {
        val errorResponse = BasicErrorResponse(
            status = ex.status,
            message = ex.message?: defaultExceptionMessages.exceptionOccurred(),
        )

        return ResponseEntity(errorResponse, HttpStatus.valueOf(ex.status))
    }

    /**
     * Handles validation exceptions from method argument validation.
     *
     * Processes [MethodArgumentNotValidException] which occurs when validation on
     * a method argument annotated with `@Valid` fails. Returns a user-friendly message
     * in Russian guiding the user to check their input data, without exposing
     * specific validation details that might leak implementation information.
     *
     * @param ex The validation exception
     * @return ResponseEntity with HTTP 400 Bad Request and a generic validation message
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<BasicErrorResponse> {
        val errorResponse = BasicErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = defaultExceptionMessages.badValidation()
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    /**
     * Handles JWT authentication exceptions.
     *
     * Processes [JwtAuthenticationException] which occurs during JWT token
     * validation failures, such as expired tokens, invalid tokens, or missing tokens.
     * Returns a 401 Unauthorized response with the exception message.
     *
     * @param ex The JWT authentication exception
     * @return ResponseEntity with HTTP 401 Unauthorized and the exception message
     */
    @ExceptionHandler(JwtAuthenticationException::class)
    fun handleJwtAuthenticationException(ex: JwtAuthenticationException): ResponseEntity<BasicErrorResponse> {
        val errorResponse = ex.message?.let {
            BasicErrorResponse(
                status = HttpStatus.UNAUTHORIZED.value(),
                message = it
            )
        }

        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    /**
     * Handles JWT signature verification exceptions.
     *
     * Processes [SignatureException] which occurs when the JWT token's signature
     * cannot be verified (e.g., token was tampered with, wrong signing key).
     * Returns a 423 Locked response with a generic message about JWT verification failure.
     *
     * @param ex The signature exception
     * @return ResponseEntity with HTTP 423 Locked and a message about invalid JWT signature
     */
    @ExceptionHandler(SignatureException::class)
    fun handleSignatureException(ex: SignatureException): ResponseEntity<BasicErrorResponse> {
        val errorResponse = ex.message?.let {
            BasicErrorResponse(
                status = HttpStatus.LOCKED.value(),
                message = defaultExceptionMessages.badJWTVerification()
            )
        }

        return ResponseEntity(errorResponse, HttpStatus.LOCKED)
    }

    /**
     * Handles authorization denial exceptions.
     *
     * Processes [AuthorizationDeniedException] which occurs when an authenticated
     * user attempts to access a resource they don't have permission for.
     * Returns a 403 Forbidden response indicating access denial.
     *
     * @param ex The authorization denied exception
     * @return ResponseEntity with HTTP 403 Forbidden and access denied message
     */
    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(ex: AuthorizationDeniedException): ResponseEntity<BasicErrorResponse> {
        val errorResponse = ex.message?.let {
            BasicErrorResponse(
                status = HttpStatus.FORBIDDEN.value(),
                message = defaultExceptionMessages.accessDenied()
            )
        }

        return ResponseEntity(errorResponse, HttpStatus.FORBIDDEN)
    }
}