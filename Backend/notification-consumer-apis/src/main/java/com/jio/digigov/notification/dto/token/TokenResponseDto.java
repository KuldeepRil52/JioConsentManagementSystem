package com.jio.digigov.notification.dto.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponseDto {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private Integer expiresIn;
    
    @JsonProperty("scope")
    private String scope;
    
    @JsonProperty("issued_at")
    private String issuedAt;
    
    // Error fields
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("error_description")
    private String errorDescription;
    
    // Success indicator
    @Builder.Default
    private boolean success = true;
}