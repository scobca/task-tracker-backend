package org.esc.tasktracker.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for database connection settings.
 *
 * This class maps database configuration from the application's property files using the prefix "database".
 * It centralizes all database connection parameters, providing type-safe access to configuration values
 * throughout the application.
 *
 * @property url The JDBC URL for the database connection.
 *               Format varies by database type.
 * @property username The username used to authenticate with the database.
 * @property password The password used to authenticate with the database.
 * @property schema The database schema to use for the connection.
 *                  This determines the default schema for unqualified table names.
 *
 * @see ConfigurationProperties
 * @see javax.sql.DataSource
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "database")
open class DatabaseProperties {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
    lateinit var schema: String
}