package com.jio.digigov.auditmodule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;

/**
 * DTO representing Consent data (without persistence annotations).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentDto implements Serializable {
    private String id;
    private String consentId;
    private String businessId;
    private String consentJsonString;
    private String payloadHash;
    private String previousChain;
    private String currentChainHash;
    private String encryptedReferenceId;
}
