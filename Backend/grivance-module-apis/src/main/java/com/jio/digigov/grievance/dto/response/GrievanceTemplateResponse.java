package com.jio.digigov.grievance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.grievance.dto.LANGUAGE;
import com.jio.digigov.grievance.dto.LanguageTypographySettings;
import com.jio.digigov.grievance.dto.request.GrievanceTemplateRequest;
import com.jio.digigov.grievance.enumeration.Status;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrievanceTemplateResponse {

    private String grievanceTemplateId;
    private String grievanceTemplateName;
    private String businessId;
    private Status status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Dynamic languages: ENGLISH, HINDI, URDU etc.
    private Map<String, LanguageContent> languages;

    private Multilingual multilingual;
    private UiConfig uiConfig;

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
    public static class Multilingual {
        private Boolean enabled;
        private List<String> supportedLanguages;
        private List<GrievanceInfo> grievanceInformation;
        private List<UserInformation> userInformation;
        private Boolean descriptionCheck;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
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