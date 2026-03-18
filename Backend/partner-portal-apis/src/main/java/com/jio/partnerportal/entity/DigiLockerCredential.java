package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document("digilocker_credentials")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DigiLockerCredential extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    
    @Schema(description = "Unique identifier for the DigiLocker credential", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("credentialId")
    private String credentialId;
    
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
    
    @Schema(description = "ID of the business associated with the credential", example = "yourBusinessId")
    @JsonProperty("businessId")
    private String businessId;
    
    @Schema(description = "Scope level of the credential", example = "TENANT", allowableValues = {"TENANT", "BUSINESS"})
    @JsonProperty("scopeLevel")
    private String scopeLevel;
    
    @Schema(description = "Status of the credential", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @JsonProperty("status")
    private String status;
}
