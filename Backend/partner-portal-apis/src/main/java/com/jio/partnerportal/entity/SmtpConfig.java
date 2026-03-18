package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.SmtpDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "smtp_configurations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmtpConfig extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    @Schema(description = "Unique identifier for the SMTP configuration", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("configId")
    private String configId;
    @Schema(description = "ID of the business associated with the SMTP configuration", example = "yourBusinessId")
    @JsonProperty("businessId")
    private String businessId;
    @Schema(description = "Scope level of the SMTP configuration", example = "GLOBAL", allowableValues = {"GLOBAL", "BUSINESS", "INDIVIDUAL"})
    @JsonProperty("scopeLevel")
    private String scopeLevel; // TENANT, BUSINESS
    @Schema(description = "JSON payload containing the SMTP configuration details")
    @JsonProperty("configurationJson")
    private SmtpDetails configurationJson;

}
