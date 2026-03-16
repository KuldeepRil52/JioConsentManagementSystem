package com.jio.digigov.notification.dto.request.dpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating DPO (Data Protection Officer) configuration.
 *
 * <p>This DTO is used to update the Data Protection Officer contact information
 * for a tenant. All fields are optional - only provided fields will be updated.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * PUT /v1/dpo-configurations
 * Headers:
 *   X-Tenant-Id: tenant123
 *   X-Transaction-Id: TXN-ABC123
 *
 * Body:
 * {
 *   "configurationJson": {
 *     "email": "new.dpo@company.com",
 *     "mobile": "9876543210"
 *   }
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to update DPO (Data Protection Officer) configuration")
public class UpdateDpoConfigurationRequestDto {

    @Valid
    @Schema(description = "DPO contact information to update (only provided fields will be updated)")
    private ConfigurationJsonDto configurationJson;

    /**
     * Embedded DTO for DPO contact information updates.
     * All fields are optional for partial updates.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "DPO contact details to update")
    public static class ConfigurationJsonDto {

        @Size(min = 2, max = 100, message = "DPO name must be between 2 and 100 characters")
        @Schema(description = "Full name of the Data Protection Officer",
                example = "Jane Doe",
                minLength = 2,
                maxLength = 100)
        private String name;

        @Email(message = "DPO email must be a valid email address")
        @Schema(description = "Official email address for DPO notifications",
                example = "dpo@company.com",
                format = "email")
        private String email;

        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian number starting with 6-9")
        @Schema(description = "Contact mobile number (10-digit Indian number)",
                example = "8475487845",
                pattern = "^[6-9]\\d{9}$")
        private String mobile;

        @Size(max = 500, message = "Address cannot exceed 500 characters")
        @Schema(description = "Physical office address of the DPO",
                example = "5th Floor, ABC Tower, Bandra Kurla Complex, Mumbai - 400051",
                maxLength = 500)
        private String address;
    }
}
