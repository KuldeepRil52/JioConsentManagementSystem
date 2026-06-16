package com.jio.partnerportal.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DigiLockerCredentialRequest {

    @Schema(description = "Client ID for DigiLocker integration", example = "D78D56A4")
    @JsonProperty("clientId")
    private String clientId;
    
    @Schema(description = "Client secret for DigiLocker integration", example = "b30cdebef4e375613886")
    @JsonProperty("clientSecret")
    private String clientSecret;
    
    @Schema(description = "Redirect URI for OAuth flow", example = "http://localhost:4200/app-documents")
    @JsonProperty("redirectUri")
    private String redirectUri;
    
    @Schema(description = "Code verifier for PKCE OAuth flow", example = "rilwfcdykeqkfxavqomttdqqqzufstjlfqzjgqvogtuzriukg")
    @JsonProperty("codeVerifier")
    private String codeVerifier;
}
