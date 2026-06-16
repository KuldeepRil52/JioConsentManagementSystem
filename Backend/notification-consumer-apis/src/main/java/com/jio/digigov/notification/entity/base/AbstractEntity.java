package com.jio.digigov.notification.entity.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Abstract base entity for MongoDB documents.
 * Provides common fields for all notification module entities.
 *
 * @author Notification Service Team
 * @since 2025-01-09
 */
@Data
@EqualsAndHashCode(of = "id")
public abstract class AbstractEntity {

    @Id
    private String id;

    @Field("createdAt")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updatedAt")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
