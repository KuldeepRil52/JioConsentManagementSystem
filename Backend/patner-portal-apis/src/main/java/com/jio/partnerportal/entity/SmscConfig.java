package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.SmscDetails;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "smsc_configurations")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmscConfig extends AbstractEntity{

    @Id
    private ObjectId id;
    private String configId;
    private String businessId;
    private String scopeLevel; // TENANT, BUSINESS
    private SmscDetails configurationJson;

}
