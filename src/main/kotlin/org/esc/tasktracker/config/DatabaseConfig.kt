package org.esc.tasktracker.config

import org.esc.tasktracker.config.properties.DatabaseProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

/**
 * Spring configuration class for database connection setup.
 *
 * This configuration class creates and configures the primary [DataSource] bean for the application.
 * It uses [DatabaseProperties] to inject database connection parameters, ensuring that all
 * database configuration is centralized and type-safe.
 *
 * @param dbProperties Injected configuration properties containing database connection details
 *                    (URL, username, password, schema)
 *
 * @see DataSource
 * @see DriverManagerDataSource
 * @see DatabaseProperties
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@Configuration
open class DatabaseConfig(private val dbProperties: DatabaseProperties) {

    @Bean
    open fun dataSource(): DataSource {
        val dataSource = DriverManagerDataSource()

        dataSource.url = dbProperties.url
        dataSource.username = dbProperties.username
        dataSource.password = dbProperties.password
        dataSource.schema = dbProperties.schema

        return dataSource
    }
}