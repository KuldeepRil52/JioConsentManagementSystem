package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.SpocDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LegalEntityUpdateRequest {

    @Schema(description = "ID of the legal entity", example = "78874edf-4b81-4047-a469-1d54af12c7e3")
    @JsonProperty("legalEntityId")
    private String legalEntityId;
    @Schema(description = "Name of the company", example = "Jio Platforms Ltd.")
    @JsonProperty("companyName")
    private String companyName;
    @Schema(description = "URL of the company logo", example = "https://example.com/logo.png")
    @JsonProperty("logoUrl")
    private String logoUrl;
    @Schema(description = "Single point of contact details")
    @JsonProperty("spoc")
    private SpocDto spoc;

}
