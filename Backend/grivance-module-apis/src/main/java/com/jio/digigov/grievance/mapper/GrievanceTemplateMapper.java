package com.jio.digigov.grievance.mapper;

import com.jio.digigov.grievance.dto.request.GrievanceTemplateRequest;
import com.jio.digigov.grievance.dto.response.GrievanceTemplateResponse;
import com.jio.digigov.grievance.entity.GrievanceTemplate;

import java.util.*;
import java.util.stream.Collectors;

public class GrievanceTemplateMapper {

    // ---------------------- REQUEST → ENTITY ----------------------
    public static GrievanceTemplate toEntity(GrievanceTemplateRequest request) {
        return GrievanceTemplate.builder()
                .grievanceTemplateId(UUID.randomUUID().toString())
                .grievanceTemplateName(request.getGrievanceTemplateName())
                .status(request.getStatus())
                .multilingual(toEntityMultilingual(request.getMultilingual()))
                .languages(request.getLanguages() != null && !request.getLanguages().isEmpty() ?
                        request.getLanguages().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> GrievanceTemplate.LanguageContent.builder()
                                                .heading(e.getValue().getHeading())
                                                .description(e.getValue().getDescription())
                                                .build()
                                ))
                        : null)
                .uiConfig(toEntityUiConfig(request.getUiConfig()))
                .build();
    }

    private static GrievanceTemplate.Multilingual toEntityMultilingual(GrievanceTemplateRequest.Multilingual req) {
        if (req == null) return null;
        return GrievanceTemplate.Multilingual.builder()
                .enabled(req.getEnabled())
                .supportedLanguages(req.getSupportedLanguages())
                .uploadFiles(req.getUploadFiles())
                .descriptionCheck(req.getDescriptionCheck())
                .grievanceInformation(req.getGrievanceInformation() == null ? null :
                        req.getGrievanceInformation().stream()
                                .map(info -> GrievanceTemplate.GrievanceInfo.builder()
                                        .grievanceType(info.getGrievanceType())
                                        .grievanceItems(info.getGrievanceItems())
                                        .build())
                                .toList())
                .userInformation(req.getUserInformation() == null ? null :
                        req.getUserInformation().stream()
                                .map(ui -> GrievanceTemplate.UserInformation.builder()
                                        .userType(ui.getUserType())
                                        .userItems(ui.getUserItems())
                                        .build())
                                .toList())
                .build();
    }

    private static GrievanceTemplate.UiConfig toEntityUiConfig(GrievanceTemplateRequest.UiConfig ui) {
        if (ui == null) return null;
        return GrievanceTemplate.UiConfig.builder()
                .logo(ui.getLogo())
                .theme(ui.getTheme())
                .logoName(ui.getLogoName())
                .darkMode(ui.getDarkMode())
                .mobileView(ui.getMobileView())
                .typographySettings(ui.getTypographySettings())
                .build();
    }

    // ---------------------- ENTITY → RESPONSE ----------------------
    public static GrievanceTemplateResponse toResponse(GrievanceTemplate entity) {
        return GrievanceTemplateResponse.builder()
                .grievanceTemplateId(entity.getGrievanceTemplateId())
                .grievanceTemplateName(entity.getGrievanceTemplateName())
                .businessId(entity.getBusinessId())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .languages(toResponseLanguages(entity.getLanguages()))
                .multilingual(toResponseMultilingual(entity.getMultilingual()))
                .uiConfig(toResponseUiConfig(entity.getUiConfig()))
                .build();
    }

    private static Map<String, GrievanceTemplateResponse.LanguageContent> toResponseLanguages(
            Map<String, GrievanceTemplate.LanguageContent> input) {
        if (input == null || input.isEmpty()) return null;

        return input.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> GrievanceTemplateResponse.LanguageContent.builder()
                                .heading(e.getValue().getHeading())
                                .description(e.getValue().getDescription())
                                .build()
                ));
    }

    private static GrievanceTemplateResponse.Multilingual toResponseMultilingual(GrievanceTemplate.Multilingual entity) {
        if (entity == null) return null;
        return GrievanceTemplateResponse.Multilingual.builder()
                .enabled(entity.getEnabled())
                .supportedLanguages(entity.getSupportedLanguages())
                .uploadFiles(entity.getUploadFiles())
                .descriptionCheck(entity.getDescriptionCheck())
                .grievanceInformation(entity.getGrievanceInformation() == null ? null :
                        entity.getGrievanceInformation().stream()
                                .map(info -> GrievanceTemplateResponse.GrievanceInfo.builder()
                                        .grievanceType(info.getGrievanceType())
                                        .grievanceItems(info.getGrievanceItems())
                                        .build())
                                .toList())
                .userInformation(entity.getUserInformation() == null ? null :
                        entity.getUserInformation().stream()
                                .map(ui -> GrievanceTemplateResponse.UserInformation.builder()
                                        .userType(ui.getUserType())
                                        .userItems(ui.getUserItems())
                                        .build())
                                .toList())
                .build();
    }

    private static GrievanceTemplateResponse.UiConfig toResponseUiConfig(GrievanceTemplate.UiConfig ui) {
        if (ui == null) return null;
        return GrievanceTemplateResponse.UiConfig.builder()
                .logo(ui.getLogo())
                .theme(ui.getTheme())
                .logoName(ui.getLogoName())
                .darkMode(ui.getDarkMode())
                .mobileView(ui.getMobileView())
                .typographySettings(ui.getTypographySettings())
                .build();
    }
}