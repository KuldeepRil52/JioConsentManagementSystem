package com.jio.partnerportal.dto.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.dto.LANGUAGE;
import com.jio.partnerportal.dto.LanguageTypographySettings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDashboardFontRequest {
	
	@Schema(description = "Typography settings", example = "{\"ENGLISH\":\n{\"fontFile\":\"base64 encoded font file\",\n\"fontSize\":14,\n\"fontWeight\":300,\n\"fontStyle\": \"BOLD\"\n}}")
    @JsonProperty("typographySettings")
    private Map<LANGUAGE, LanguageTypographySettings> typographySettings;    
}

