package com.jio.vault.dto.cryptodto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "tenantId",
        "businessId",
        "dataCategoryType",
        "dataCategoryValue",
        "uuid",
        "encryptedString",
        "createdTimeStamp"
})
public class EncryptPayloadResponse {

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("businessId")
    private String businessId;

    @JsonProperty("dataCategoryType")
    private String dataCategoryType;

    @JsonProperty("dataCategoryValue")
    private String dataCategoryValue;

    @JsonProperty("referenceId")
    private String uuid;

    @JsonProperty("encryptedString")
    private String encryptedString;

    @JsonProperty("createdTimeStamp")
    private String createdTimeStamp;
}

