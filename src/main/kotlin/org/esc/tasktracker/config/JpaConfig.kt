package org.esc.tasktracker.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * JPA configuration class that enables auditing functionality for entity lifecycle events.
 *
 * This configuration class activates JPA auditing capabilities, which automatically
 * populate auditing fields on entities (such as creation date, modification date,
 * created by, modified by) when entities are persisted or updated.
 *
 * @see org.springframework.data.jpa.domain.support.AuditingEntityListener
 * @see org.springframework.data.annotation.CreatedDate
 * @see org.springframework.data.annotation.LastModifiedDate
 * @see org.springframework.data.annotation.CreatedBy
 * @see org.springframework.data.annotation.LastModifiedBy
 * @see org.springframework.data.domain.AuditorAware
 *
 * @author Vladimir Fokin
 * @since 1.0
 */
@Configuration
@EnableJpaAuditing
open class JpaConfig