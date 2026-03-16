package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Document("business_applications")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessApplication extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    @Schema(description = "Unique identifier for the business application", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("businessId")
    private String businessId;
    @Schema(description = "Name of the business application", example = "My CRM App")
    @JsonProperty("name")
    private String name;
    @Schema(description = "Description of the business application", example = "A comprehensive CRM application for managing customer relationships")
    @JsonProperty("description")
    private String description;
    @Schema(description = "Scope level of the business application", example = "TENANT", allowableValues = {"TENANT", "BUSINESS"})
    @JsonProperty("scopeLevel")
    private String scopeLevel;

}
