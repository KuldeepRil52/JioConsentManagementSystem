package com.jio.digigov.notification.dto.response.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(description = "Count response with breakdown")
public class CountResponseDto {

    @Schema(description = "Count data")
    private CountData data;

    @Data
    @Builder
    @Schema(description = "Count details with breakdown")
    public static class CountData {
        @Schema(description = "Total count", example = "45")
        private long totalCount;

        @Schema(description = "Count breakdown by various categories")
        private CountBreakdown breakdown;

        @Data
        @Builder
        @Schema(description = "Detailed count breakdown")
        public static class CountBreakdown {
            @Schema(description = "Count by status", example = "{\"ACTIVE\": 35, \"PENDING\": 5}")
            private Map<String, Integer> byStatus;

            @Schema(description = "Count by type", example = "{\"NOTIFICATION\": 30, \"OTPVALIDATOR\": 15}")
            private Map<String, Integer> byType;

            @Schema(description = "Count by channel", example = "{\"SMS\": 25, \"EMAIL\": 20}")
            private Map<String, Integer> byChannel;

            @Schema(description = "Count by event type", example = "{\"CONSENT_GRANTED\": 10, \"CONSENT_REVOKED\": 8}")
            private Map<String, Integer> byEventType;

            @Schema(description = "Count by language", example = "{\"english\": 25, \"hindi\": 15}")
            private Map<String, Integer> byLanguage;
        }
    }
}