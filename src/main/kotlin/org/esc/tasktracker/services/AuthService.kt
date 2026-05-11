package org.esc.tasktracker.services

import org.esc.tasktracker.dto.auth.LoginUserDto
import org.esc.tasktracker.dto.auth.UpdateUserTokensDto
import org.esc.tasktracker.dto.jwt.CreateJwtToken
import org.esc.tasktracker.dto.jwt.UserTokensDto
import org.esc.tasktracker.dto.users.CreateUserDto
import org.esc.tasktracker.entities.Users
import org.esc.tasktracker.exceptions.JwtAuthenticationException
import org.esc.tasktracker.security.JwtUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service responsible for handling authentication operations including user registration,
 * login, and token management.
 *
 * This service provides methods for user authentication, token generation, and token refresh
 * operations. It integrates with [UsersService] for user management, [JwtUtil] for JWT token
 * operations, and [PasswordEncoder] for password validation.
 *
 * @property usersService CRUD Service for user-related operations
 * @property jwtUtil Utility for JWT token generation and validation
 * @property passwordEncoder Encoder for password verification
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@Service
open class AuthService(
    private val usersService: UsersService,
    private val jwtUtil: JwtUtil,
    private val passwordEncoder: PasswordEncoder,
) {

    /**
     * Registers a new user and generates authentication tokens.
     *
     * This method creates a new user account using the provided registration data,
     * then generates and returns access and refresh tokens for the newly created user.
     *
     * @param item The user registration data containing name, email, password user details
     * @param sessionId Unique identifier for the user session
     * @return [UserTokensDto] containing the generated access and refresh tokens
     */
    fun register(item: CreateUserDto, sessionId: UUID): UserTokensDto {
        return generateTokens(usersService.create(item), sessionId)
    }

    /**
     * Authenticates a user and generates authentication tokens.
     *
     * This method validates user credentials by:
     * 1. Retrieving the user by email
     * 2. Verifying the provided password matches the stored hash
     * 3. Generating new access and refresh tokens upon successful authentication
     *
     * @param item The login credentials containing email and password
     * @param sessionId Unique identifier for the user session
     * @return [UserTokensDto] containing the generated access and refresh tokens
     * @throws JwtAuthenticationException If the email doesn't exist or password is incorrect
     */
    fun login(item: LoginUserDto, sessionId: UUID): UserTokensDto {
        return usersService.getByEmail(item.email, throwable = false)?.let { user ->
            if (!passwordEncoder.matches(item.password, user.password)) {
                throw JwtAuthenticationException("Неверные логин или пароль.")
            }
            generateTokens(user, sessionId)
        } ?: throw JwtAuthenticationException("Неверные логин или пароль.")
    }

    /**
     * Refreshes authentication tokens using a valid refresh token.
     *
     * This method performs the following operations:
     * 1. Verifies the access token without time limit constraints
     * 2. Validates the refresh token against the session
     * 3. Extracts user information from the refresh token
     * 4. Generates new tokens for the authenticated user
     *
     * The operation is transactional to ensure data consistency.
     *
     * @param data Contains the current access and refresh tokens
     * @param sessionId Unique identifier for the user session
     * @return [UserTokensDto] containing the new access and refresh tokens
     * @throws JwtAuthenticationException If either token is invalid or user cannot be extracted from refresh token
     */
    @Transactional
    fun updateTokens(data: UpdateUserTokensDto, sessionId: UUID): UserTokensDto {
        jwtUtil.verifyToken(data.accessToken, throwTimeLimit = false)
        jwtUtil.verifyToken(data.refreshToken, sessionId)

        return jwtUtil.getUserFromToken(data.refreshToken)
            ?.let { user -> generateTokens(user, sessionId) }
            ?: throw JwtAuthenticationException("Неверный refresh-токен.")
    }

    /**
     * Generates new access and refresh tokens for a user.
     *
     * This private helper method:
     * 1. Removes any existing refresh token associated with the session
     * 2. Generates a new access token with short expiration
     * 3. Generates a new refresh token with longer expiration
     *
     * @param user The user entity for whom tokens are being generated
     * @param sessionId Unique identifier for the current session
     * @return [UserTokensDto] containing the newly generated tokens
     */
    private fun generateTokens(user: Users, sessionId: UUID): UserTokensDto {
        jwtUtil.removeOldRefreshTokenByUUID(sessionId)
        return UserTokensDto(
            accessToken = jwtUtil.generateAccessToken(CreateJwtToken(user, sessionId)),
            refreshToken = jwtUtil.generateRefreshToken(CreateJwtToken(user, sessionId))
        )
    }
}