package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "user_dashboard_themes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDashboardTheme extends AbstractEntity {

    @Id
    @JsonProperty("id")
    private ObjectId id;
    
    @Schema(description = "Unique identifier for the user dashboard theme", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("themeId")
    private String themeId;
    
    @Schema(description = "ID of the tenant", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("tenantId")
    private String tenantId;
    
    @Schema(description = "ID of the business", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    @JsonProperty("businessId")
    private String businessId;
    
    @Schema(description = "Theme configuration as JSON string", example = "{\"primaryColor\":\"#007bff\",\"secondaryColor\":\"#6c757d\"}")
    @JsonProperty("theme")
    private String theme;
}

