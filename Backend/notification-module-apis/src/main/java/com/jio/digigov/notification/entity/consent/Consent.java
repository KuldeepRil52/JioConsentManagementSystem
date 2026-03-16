package com.jio.digigov.notification.entity.consent;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.notification.entity.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Consent entity representing consent records in the consents collection.
 * Used to retrieve consent template information for the consent deletion details API.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "consents")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@CompoundIndexes({
    @CompoundIndex(name = "consent_business_idx", def = "{'consentId': 1, 'businessId': 1}")
})
public class Consent extends BaseEntity {

    @Field("consentId")
    private String consentId;

    @Field("consentHandleId")
    private String consentHandleId;

    @Field("templateId")
    private String templateId;

    @Field("templateName")
    private String templateName;

    @Field("templateVersion")
    private int templateVersion;

    @Field("businessId")
    private String businessId;

    @Field("languagePreferences")
    private String languagePreferences;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @Field("startDate")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @Field("endDate")
    private LocalDateTime endDate;

    @Field("status")
    private String status;

    @Field("staleStatus")
    private String staleStatus;

    @Field("parentalKyc")
    private String parentalKyc;

    @Field("parentalReferenceId")
    private String parentalReferenceId;

    @Field("isParentalConsent")
    @Builder.Default
    private Boolean isParentalConsent = false;
}
