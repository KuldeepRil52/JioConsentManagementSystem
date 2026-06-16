package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.ConsentDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "consent_configurations")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentConfig extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    @Schema(description = "Unique identifier for the consent configuration", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("configId")
    private String configId;
    @Indexed(unique = true)
    @Schema(description = "ID of the business associated with the consent configuration", example = "yourBusinessId")
    @JsonProperty("businessId")
    private String businessId;
    @Schema(description = "Scope level of the consent configuration", example = "GLOBAL", allowableValues = {"GLOBAL", "BUSINESS", "INDIVIDUAL"})
    @JsonProperty("scopeLevel")
    private String scopeLevel;
    @Schema(description = "Details of the consent configuration")
    @JsonProperty("configurationJson")
    private ConsentDetails configurationJson;

}
