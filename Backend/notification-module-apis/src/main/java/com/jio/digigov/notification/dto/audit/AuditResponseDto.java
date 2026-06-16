package com.jio.digigov.notification.dto.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Audit Response DTO for receiving responses from central audit service.
 * This is used primarily for logging and debugging purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditResponseDto {

    private String auditId;
    private String status;
    private String message;
    private Long timestamp;
}
