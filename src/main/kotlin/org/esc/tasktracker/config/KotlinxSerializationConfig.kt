package org.esc.tasktracker.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter

/**
 * Configuration class for Kotlinx Serialization HTTP message conversion.
 *
 * This configuration sets up [KotlinSerializationJsonHttpMessageConverter] as a message converter
 * for HTTP requests and responses, enabling Kotlinx Serialization to handle JSON serialization
 * and deserialization in Spring MVC controllers.
 *
 * @see KotlinSerializationJsonHttpMessageConverter
 * @see Json
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@Configuration
open class KotlinxSerializationConfig {

    /**
     * Creates and configures a [KotlinSerializationJsonHttpMessageConverter] bean.
     *
     * This message converter is automatically registered in Spring MVC's HTTP message
     * converters and handles JSON serialization/deserialization for controller methods
     * with `@RequestBody`, `@ResponseBody`, or `ResponseEntity` return types.
     *
     * The converter is configured with:
     * - **ignoreUnknownKeys = true**: JSON properties not present in Kotlin classes
     *   are silently ignored instead of throwing exceptions
     * - **encodeDefaults = true**: Properties with default values are included in
     *   the serialized output, maintaining consistent JSON structure
     * - **prettyPrint = true**: Human-readable formatted JSON output (consider
     *   disabling in production for smaller payloads)
     *
     * @return Configured [KotlinSerializationJsonHttpMessageConverter] instance
     *
     * @see KotlinSerializationJsonHttpMessageConverter
     * @see Json
     */
    @Bean
    open fun kotlinSerializationJsonHttpMessageConverter(): KotlinSerializationJsonHttpMessageConverter {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }
        return KotlinSerializationJsonHttpMessageConverter(json)
    }
}