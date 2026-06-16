package com.jio.digigov.auditmodule.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * DTO for representing BusinessApplication info fetched by businessId.
 */
@Data
public class BusinessApplicationDto {
    private String businessId;
    private String name;
    private String description;
    private String scopeLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}