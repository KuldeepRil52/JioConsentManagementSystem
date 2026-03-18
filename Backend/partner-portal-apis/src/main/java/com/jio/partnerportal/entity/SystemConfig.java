package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.SmtpDetails;
import com.jio.partnerportal.dto.SystemDetails;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "system_configurations")
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemConfig extends AbstractEntity{

    @Id
    private ObjectId id;
    private String configId;
    private String businessId;
    private String scopeLevel;
    private SystemDetails configurationJson;

}
