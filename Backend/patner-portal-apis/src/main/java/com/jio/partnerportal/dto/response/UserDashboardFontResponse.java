package com.jio.partnerportal.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.partnerportal.dto.LANGUAGE;
import com.jio.partnerportal.dto.LanguageTypographySettings;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDashboardFontResponse {
    private String message;
    private String fontId;
    private Map<LANGUAGE, LanguageTypographySettings> typographySettings;
}

