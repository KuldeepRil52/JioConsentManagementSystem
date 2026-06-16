package com.example.scanner.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptPayloadResponse {
    private String tenantId;
    private String businessId;
    private String dataCategoryType;
    private String dataCategoryValue;
    private String referenceId;
    private String encryptedString;
    private String createdTimeStamp;
}

