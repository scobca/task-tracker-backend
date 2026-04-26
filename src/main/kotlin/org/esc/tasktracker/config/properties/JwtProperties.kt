package org.esc.tasktracker.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for JWT (JSON Web Token) settings.
 *
 * This class maps JWT configuration from the application's property files
 * (application.yml, application.properties) using the prefix "config.jwt".
 * It centralizes all JWT-related parameters, providing type-safe access to
 * token configuration values throughout the application.
 *
 * @property secret The secret key used for signing and verifying JWT tokens.
 *                  This should be a strong, secure string that is kept confidential.
 * @property accessTokenExpiration The expiration time for access tokens.
 *                                 Typically, shorter-lived (minutes to hours).
 * @property refreshTokenExpiration The expiration time for refresh tokens.
 *                                  Typically, longer-lived (days to weeks).
 *
 * @see ConfigurationProperties
 * @see io.jsonwebtoken.Jwts
 * @see io.jsonwebtoken.SignatureAlgorithm
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "config.jwt")
open class JwtProperties {
    lateinit var secret: String
    lateinit var accessTokenExpiration: String
    lateinit var refreshTokenExpiration: String

}