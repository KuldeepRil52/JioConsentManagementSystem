package com.jio.schedular.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.schedular.enums.ConsentHandleRemarks;
import com.jio.schedular.enums.ConsentHandleStatus;
import com.jio.schedular.dto.CustomerIdentifiers;
import com.jio.schedular.enums.ParentalKycType;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@org.springframework.data.mongodb.core.mapping.Document("consent_handles")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ConsentHandle extends AbstractEntity {

    @Id
    private ObjectId id;
    private String consentHandleId;
    private String businessId;
    private String txnId;
    private String templateId;
    private int templateVersion;
    private CustomerIdentifiers customerIdentifiers;
    private String consentId;
    private ConsentHandleStatus status;
    private Boolean isParentalConsent;
    private ParentalKycType parentalKYCType;
    private String code;
    private String state;
    private ConsentHandleRemarks remarks;
}