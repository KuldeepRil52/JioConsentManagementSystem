package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "data_types")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataType extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    @Schema(description = "Unique identifier for the data type", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("dataTypeId")
    private String dataTypeId;
    @Schema(description = "Name of the data type", example = "Email Address")
    @JsonProperty("dataTypeName")
    private String dataTypeName;
    @Schema(description = "List of data items associated with this data type", example = "[\"emailId\", \"contactEmail\"]")
    @JsonProperty("dataItems")
    private List<String> dataItems;
    @Schema(description = "ID of the business associated with the data type", example = "yourBusinessId")
    @JsonProperty("businessId")
    private String businessId;
    @Schema(description = "Scope type of the data type", example = "GLOBAL", allowableValues = {"GLOBAL", "BUSINESS", "INDIVIDUAL"})
    @JsonProperty("scopeType")
    private String scopeType;
    @Schema(description = "Status of the data type", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @JsonProperty("status")
    private String status;

}
