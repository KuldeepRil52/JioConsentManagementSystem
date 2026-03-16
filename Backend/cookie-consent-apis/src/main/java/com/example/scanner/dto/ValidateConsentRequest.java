package com.example.scanner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateConsentRequest {
    private String consentToken;
    private String jwsToken;  // JWS token body me aayega
}