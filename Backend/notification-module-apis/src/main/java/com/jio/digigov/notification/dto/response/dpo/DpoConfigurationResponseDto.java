package com.jio.digigov.notification.dto.response.dpo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for DPO (Data Protection Officer) configuration.
 *
 * <p>This DTO is returned when fetching DPO configuration information.
 * It includes the contact details and metadata like creation/update timestamps.</p>
 *
 * <p><b>Example Response:</b></p>
 * <pre>
 * {
 *   "id": "65f7a8b9c1d2e3f4a5b6c7d8",
 *   "configurationJson": {
 *     "name": "Jane Doe",
 *     "email": "dpo@company.com",
 *     "mobile": "8475487845",
 *     "address": "5th Floor, ABC Tower, Mumbai - 400051"
 *   },
 *   "createdAt": "2025-01-15T10:30:00",
 *   "updatedAt": "2025-01-15T10:30:00"
 * }
 * </pre>
 *
 * @since 2.0.0
 * @author DPDP Notification Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DPO (Data Protection Officer) configuration response")
public class DpoConfigurationResponseDto {

    @Schema(description = "Unique identifier for the DPO configuration",
            example = "65f7a8b9c1d2e3f4a5b6c7d8")
    private String id;

    @Schema(description = "DPO contact information")
    private ConfigurationJsonDto configurationJson;

    @Schema(description = "Timestamp when the configuration was created",
            example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the configuration was last updated",
            example = "2025-01-15T10:30:00")
    private LocalDateTime updatedAt;

    /**
     * Embedded DTO for DPO contact information in the response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "DPO contact details")
    public static class ConfigurationJsonDto {

        @Schema(description = "Full name of the Data Protection Officer",
                example = "Jane Doe")
        private String name;

        @Schema(description = "Official email address for DPO notifications",
                example = "dpo@company.com")
        private String email;

        @Schema(description = "Contact mobile number",
                example = "8475487845")
        private String mobile;

        @Schema(description = "Physical office address of the DPO",
                example = "5th Floor, ABC Tower, Bandra Kurla Complex, Mumbai - 400051")
        private String address;
    }
}
