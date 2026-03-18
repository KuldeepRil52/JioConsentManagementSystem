package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.SpocDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Document("legal_entities")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegalEntity extends AbstractEntity {

    @Id
    private ObjectId id;
    private String legalEntityId;
    private String companyName;
    private String logoUrl;
    private SpocDto spoc;
    @JsonIgnore
    private String wso2OnboardName;
}
