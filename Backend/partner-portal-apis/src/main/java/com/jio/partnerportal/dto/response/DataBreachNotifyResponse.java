package com.jio.partnerportal.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response body for Data Breach Notification")
    public class DataBreachNotifyResponse {
        private String incidentId;
        private String tenantId;
        private String notifyGroupId;

        @Schema(description = "Record creation time", example = "2025-10-31T10:19:37.955+00:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        private LocalDateTime createdAt;
        
        private String status;
    }