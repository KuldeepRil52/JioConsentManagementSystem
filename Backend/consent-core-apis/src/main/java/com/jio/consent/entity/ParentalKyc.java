package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import com.jio.consent.dto.ParentalKycType;
import com.jio.consent.dto.ParentalKycStatus;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@org.springframework.data.mongodb.core.mapping.Document("kyc")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ParentalKyc extends AbstractEntity {

    @Id
    private ObjectId id;
    private String tenantId;
    private String businessId;
    private String name;
    private String mobileNo;
    private String dob;
    private ParentalKycType type;
    private ParentalKycStatus status;
    private String kycReferenceId;
    private String parentalReferenceId;
    private String aadhaarEncryptedData;

}
