package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.PremiumMeta;
import com.jio.partnerportal.dto.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "tenant_registry")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class TenantRegistry extends AbstractEntity {

    @Id
    private ObjectId id;
    @Indexed(unique = true)
    private String tenantId;
    private String pan;
    private String clientId;
    private Status status;
    private boolean isPremium = false;
    private PremiumMeta premiumMeta;
    private String onboardingUserId;

}
