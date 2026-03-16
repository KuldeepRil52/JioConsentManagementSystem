package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.ConsentHandleRemarks;
import com.jio.consent.dto.ConsentHandleStatus;
import com.jio.consent.dto.CustomerIdentifiers;
import jdk.jfr.Unsigned;
import lombok.*;
import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@org.springframework.data.mongodb.core.mapping.Document("consent_handles")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ConsentHandle extends AbstractEntity{

    @Id
    private ObjectId id;
    @Indexed(unique = true, name = "consentHandleId")
    private String consentHandleId;
    private String businessId;
    private String txnId;
    @Indexed(name = "templateId")
    private String templateId;
    @Indexed(name = "templateVersion")
    private int templateVersion;
    private CustomerIdentifiers customerIdentifiers;
    private String consentId;
    private ConsentHandleStatus status;
    @Builder.Default
    private ConsentHandleRemarks remarks = ConsentHandleRemarks.DATA_FIDUCIARY;
    // Parental consent fields
    private Boolean isParental;
    private String parentIdentity;
    private String parentIdentityType;
    private String parentName;

}
