package com.jio.digigov.grievance.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.digigov.grievance.dto.LANGUAGE;
import com.jio.digigov.grievance.dto.LanguageTypographySettings;
import com.jio.digigov.grievance.enumeration.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrievanceTemplateRequest {

    private String grievanceTemplateName;

    private Status status;  // DRAFT | PUBLISHED

    private Multilingual multilingual;

    private UiConfig uiConfig;

    @Builder.Default
    private Map<String, LanguageContent> languages = new HashMap<>();

    /**
     * Only adds top-level language fields if they are present in supportedLanguages.
     */
    @JsonAnySetter
    public void addLanguage(String key, Object value) {
        // Ignore known fields
        if ("grievanceTemplateName".equals(key)
                || "businessId".equals(key)
                || "status".equals(key)
                || "multilingual".equals(key)
                || "uiConfig".equals(key)) {
            return;
        }

        if (multilingual != null && multilingual.getSupportedLanguages() != null
                && multilingual.getSupportedLanguages().contains(key.toUpperCase())) {
            if (value instanceof Map<?, ?> map) {
                Object heading = map.get("heading");
                Object description = map.get("description");
                this.languages.put(key.toUpperCase(), LanguageContent.builder()
                        .heading(heading != null ? heading.toString() : null)
                        .description(description != null ? description.toString() : null)
                        .build());
            }
        } else {
            // optional: throw exception if language is not allowed
            // throw new IllegalArgumentException("Language " + key + " is not in supportedLanguages");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Multilingual {

        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("supportedLanguages")
        private List<String> supportedLanguages;

        @JsonProperty("grievanceInformation")
        private List<GrievanceInfo> grievanceInformation;

        @JsonProperty("userInformation")
        private List<UserInformation> userInformation;

        @JsonProperty("descriptionCheck")
        private Boolean descriptionCheck;

        @JsonProperty("uploadFiles")
        private Boolean uploadFiles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GrievanceInfo {
        private String grievanceType;
        private List<String> grievanceItems;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInformation {
        private List<String> userType;
        private List<String> userItems;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LanguageContent {
        private String heading;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UiConfig {
        private String logo;
        private String theme;
        private String logoName;
        private Boolean darkMode;
        private Boolean mobileView;
        private Map<LANGUAGE, LanguageTypographySettings> typographySettings;
    }
    
//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    public static class LanguageTypographySettings {
//    	private String fontFile;
//    	private Integer fontSize;
//    	private Integer fontWeight;
//    	private String fontStyle;
//    }
}