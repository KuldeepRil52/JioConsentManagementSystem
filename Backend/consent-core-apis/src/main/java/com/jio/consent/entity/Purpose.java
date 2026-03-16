package com.jio.consent.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document("purposes")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Purpose extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    @Schema(description = "Unique identifier for the purpose", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("purposeId")
    private String purposeId;
    @Schema(description = "Code for the purpose", example = "MKTG_CAMPAIGN")
    @JsonProperty("purposeCode")
    private String purposeCode;
    @Schema(description = "Name of the purpose", example = "Marketing Campaign Data")
    @JsonProperty("purposeName")
    private String purposeName;
    @Schema(description = "Description of the purpose", example = "Collecting data for targeted marketing campaigns.")
    @JsonProperty("purposeDescription")
    private String purposeDescription;
    @Schema(description = "ID of the business associated with the purpose", example = "yourBusinessId")
    @JsonProperty("businessId")
    private String businessId;
    @Schema(description = "Scope type of the purpose", example = "GLOBAL", allowableValues = {"GLOBAL", "BUSINESS", "INDIVIDUAL"})
    @JsonProperty("scopeType")
    private String scopeType;
    @Schema(description = "Status of the purpose", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @JsonProperty("status")
    private String status;

}
