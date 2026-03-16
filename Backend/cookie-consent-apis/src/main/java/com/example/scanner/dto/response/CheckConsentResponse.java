package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response after checking consent status")
public class CheckConsentResponse {
    @Schema(
            description = "Consent handle or Consent response.",
            example = "Pending, Req_Expired, Active, Revoke, Reject, Expired, No_Record"
    )
    String consentStatus;

    @Schema(
            description = "Consent handle ID - UUID",
            example = "123e4567-XXXXXX-4266XXXX..."
    )
    String consentHandleId;
}
