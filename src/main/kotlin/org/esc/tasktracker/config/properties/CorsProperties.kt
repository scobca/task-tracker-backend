package org.esc.tasktracker.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for CORS (Cross-Origin Resource Sharing) settings.
 *
 * This class maps CORS configuration from the application's property files.
 *
 * @property frontendLocalHost The URL of the frontend application running locally
 *                            (typically used in development environments)
 * @property frontendDockerHost The URL of the frontend application running in Docker
 *                             (used when both backend and frontend are containerized)
 * @property frontendProductionHost The URL of the frontend application in production
 *                                 (the live application URL)
 *
 * @see ConfigurationProperties
 * @see org.springframework.web.servlet.config.annotation.CorsRegistry
 * @see org.springframework.web.cors.CorsConfiguration
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "config.cors")
open class CorsProperties {
    lateinit var frontendLocalHost: String
    lateinit var frontendDockerHost: String
    lateinit var frontendProductionHost: String
}