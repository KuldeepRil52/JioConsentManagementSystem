package com.jio.digigov.grievance.dto;

import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Represents status history of grievance with remark and timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusTimeline {
    private GrievanceStatus status;
    private LocalDateTime timestamp;
    private String remark;
    private LocalDateTime updatedAt;
}
