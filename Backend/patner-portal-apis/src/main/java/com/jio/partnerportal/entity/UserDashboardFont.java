package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.LANGUAGE;
import com.jio.partnerportal.dto.LanguageTypographySettings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "user_dashboard_fonts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDashboardFont extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    
    @Schema(description = "Unique identifier for the user dashboard font", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("font")
    private String fontId;
    
    @Schema(description = "ID of the tenant", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("tenantId")
    private String tenantId;
    
    @Schema(description = "ID of the business", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("businessId")
    private String businessId;
    
    @Schema(description = "Typography settings", example = "{\"ENGLISH\":\n{\"fontFile\":\"base64 encoded font file\",\n\"fontSize\":14,\n\"fontWeight\":300,\n\"fontStyle\": \"BOLD\"\n}}")
    @JsonProperty("typographySettings")
    private Map<LANGUAGE, LanguageTypographySettings> typographySettings;
}

