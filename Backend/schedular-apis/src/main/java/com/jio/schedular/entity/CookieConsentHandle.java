package com.jio.schedular.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.schedular.enums.ConsentHandleStatus;
import com.jio.schedular.dto.CustomerIdentifiers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cookie_consent_handles")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CookieConsentHandle {

    @Id
    private String id;

    @Field("consentHandleId")
    private String consentHandleId;

    @Field("businessId")
    private String businessId;

    @Field("txnId")
    private String txnId;

    @Field("templateId")
    private String templateId;

    @Field("url")
    private String url;

    @Field("templateVersion")
    private int templateVersion;

    @Field("customerIdentifiers")
    private CustomerIdentifiers customerIdentifiers;

    @Field("status")
    private ConsentHandleStatus status;

    @Field("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant createdAt;

    @Field("updatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant updatedAt;

    @Field("expiresAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    private Instant expiresAt;

    @Field("_class")
    private String className;

}