package com.jio.schedular.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jio.schedular.enums.GrievanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Represents status history of grievance with remark and timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusTimeline {
    private GrievanceStatus status;
    private OffsetDateTime timestamp;
    private String remark;
    private OffsetDateTime updatedAt;
}