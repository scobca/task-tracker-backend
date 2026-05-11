package org.esc.tasktracker.dto.auth

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data transfer object for user login requests.
 *
 * Contains the credentials required to authenticate an existing user.
 * This DTO is used in the authentication flow to validate user identity
 * and issue new JWT tokens.
 *
 * @param email User's email address - used as the primary identifier for authentication.
 *              Must match an existing user in the system.
 * @param password User's password in plain text - will be validated against
 *                 the stored bcrypt hash. Never logged or persisted.
 *
 * @see org.esc.tasktracker.controllers.AuthController.login
 * @see org.esc.tasktracker.services.AuthService.login
 * @see org.esc.tasktracker.dto.users.CreateUserDto
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
data class LoginUserDto(
    @JsonProperty("email")
    val email: String,

    @JsonProperty("password")
    val password: String,
)
