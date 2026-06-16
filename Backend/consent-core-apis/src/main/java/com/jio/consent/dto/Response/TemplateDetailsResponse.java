package com.jio.consent.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.*;
import com.jio.consent.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class TemplateDetailsResponse {

    private String templateId;
    private String templateName;
    private int version;
    private String businessId;
    private TemplateStatus status;
    private Multilingual multilingual;
    private List<EnhancedPreference> preferences;
    private UiConfig uiConfig;
    private Document documentMeta;

}
