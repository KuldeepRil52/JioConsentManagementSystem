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
@Schema(description = "Response after successfully creating a consent handle")
public class ConsentHandleResponse {
    @Schema(
            description = "Unique consent handle ID - use this in consent creation",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX..."
    )
    private String consentHandleId;
    @Schema(
            description = "Success message",
            example = "Consent Handle Created successfully!"
    )
    private String message;
    @Schema(
            description = "Transaction ID from request header",
            example = "eyXXX.EXAMPLE-TOKEN-NOT-REAL.xxxXXX....."
    )
    private String txnId;

    @Schema(description = "Indicates if this is a newly created handle or existing one")
    private boolean isNewHandle;
}