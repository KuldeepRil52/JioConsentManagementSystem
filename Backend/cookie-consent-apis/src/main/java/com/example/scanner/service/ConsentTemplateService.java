package com.example.scanner.service;

import com.example.scanner.config.MultiTenantMongoConfig;
import com.example.scanner.config.TenantContext;
import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.request.CreateTemplateRequest;
import com.example.scanner.dto.LanguageTypographySettings;
import com.example.scanner.dto.Preference;
import com.example.scanner.dto.request.UpdateTemplateRequest;
import com.example.scanner.dto.response.UpdateTemplateResponse;
import com.example.scanner.entity.ConsentTemplate;
import com.example.scanner.entity.ScanResultEntity;
import com.example.scanner.enums.LANGUAGE;
import com.example.scanner.enums.PreferenceStatus;
import com.example.scanner.enums.TemplateStatus;
import com.example.scanner.enums.VersionStatus;
import com.example.scanner.exception.ConsentException;
import com.example.scanner.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.tika.Tika;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.scanner.dto.response.TemplateWithCookiesResponse;
import com.example.scanner.dto.response.PreferenceWithCookies;
import com.example.scanner.entity.CookieEntity;
import java.util.stream.Collectors;

import java.util.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentTemplateService {

    private final MultiTenantMongoConfig mongoConfig;
    private final CategoryService categoryService;
    private final AuditService auditService;

    private static final Set<String> ALLOWED_FONT_TYPES = Set.of(
            "font/ttf",
            "font/otf",
            "font/woff",
            "font/woff2",
            "application/x-font-ttf",
            "application/x-font-otf",
            "application/font-woff");

    public Optional<ConsentTemplate> getTemplateByTenantAndTemplateIdAndTemplateVersion(String tenantId, String templateId, int templateVersion) {
        validateInputs(tenantId, "Tenant ID cannot be null or empty");
        validateInputs(templateId, "template ID cannot be null or empty");

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            Query query = new Query(Criteria.where("templateId").is(templateId).and("version").is(templateVersion));
            ConsentTemplate template = tenantMongoTemplate.findOne(query, ConsentTemplate.class);
            return Optional.ofNullable(template);
        } finally {
            TenantContext.clear();
        }
    }

    public Optional<ConsentTemplate> getTemplateByTenantAndTemplateIdAndBusinessId(String tenantId, String templateId,
                                                                                    int version) {
        validateInputs(tenantId, "Tenant ID cannot be null or empty");
        validateInputs(templateId, "template ID cannot be null or empty");

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            Query query = new Query(Criteria.where("templateId").is(templateId)
                    .and("status").is("PUBLISHED").and("templateStatus").is("ACTIVE").and("version")
                    .is(version));

            ConsentTemplate template = tenantMongoTemplate.findOne(query, ConsentTemplate.class);
            return Optional.ofNullable(template);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public ConsentTemplate createTemplate(String tenantId, CreateTemplateRequest createRequest, String businessId) throws ConsentException {
        validateInputs(tenantId, "Tenant ID cannot be null or empty");
        validateCreateRequest(createRequest);

        auditService.logTemplateCreationInitiated(tenantId, businessId);

        validateScanExists(tenantId, createRequest.getScanId());

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            // Check if template already exists for this scan ID
            if (templateExistsForScan(tenantMongoTemplate, createRequest.getScanId())) {
                throw new IllegalArgumentException("Template already exists for scan ID: " + createRequest.getScanId());
            }

            // Create template from request (using the provided scanId)
            ConsentTemplate template = ConsentTemplate.fromCreateRequest(createRequest, createRequest.getScanId(), businessId);

            validateTemplatePurposes(template.getPreferences(), tenantId);

            // Process preferences and set defaults
            processPreferences(template.getPreferences());

            // Save template
            ConsentTemplate savedTemplate = tenantMongoTemplate.save(template);

            auditService.logTemplateCreated(tenantId, businessId, savedTemplate.getTemplateId());

            log.info("Successfully created template");

            return savedTemplate;

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * CRITICAL: Validate that scan exists in scan_results table and is COMPLETED
     */
    private void validateScanExists(String tenantId, String scanId) {
        if (scanId == null || scanId.trim().isEmpty()) {
            throw new IllegalArgumentException("Scan ID is required for template creation");
        }

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            Query query = new Query(Criteria.where("transactionId").is(scanId));
            ScanResultEntity scanResult = tenantMongoTemplate.findOne(query, ScanResultEntity.class);

            if (scanResult == null) {
                throw new IllegalArgumentException("Scan with ID '" + scanId + "' does not exist");
            }

            if (!"COMPLETED".equals(scanResult.getStatus())) {
                throw new IllegalArgumentException("Scan with ID '" + scanId + "' is not completed. Current status: " + scanResult.getStatus());
            }

        } finally {
            TenantContext.clear();
        }
    }

    private boolean templateExistsForScan(MongoTemplate mongoTemplate, String scanId) {
        Query query = new Query(Criteria.where("scanId").is(scanId));
        return mongoTemplate.exists(query, ConsentTemplate.class);
    }

    private void processPreferences(List<Preference> preferences) {
        if (preferences == null) return;

        LocalDateTime now = LocalDateTime.now();

        for (Preference preference : preferences) {

            if (preference.getPreferenceStatus() == null) {
                preference.setPreferenceStatus(PreferenceStatus.NOTACCEPTED);
            }

            if (preference.getStartDate() == null) {
                preference.setStartDate(now);
            }

            if (preference.getPreferenceValidity() != null && preference.getEndDate() == null) {
                LocalDateTime endDate = calculateEndDate(now, preference.getPreferenceValidity());
                preference.setEndDate(endDate);
            }
        }
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, com.example.scanner.dto.Duration duration) {
        switch (duration.getUnit()) {
            case DAYS:
                return startDate.plusDays(duration.getValue());
            case MONTHS:
                return startDate.plusMonths(duration.getValue());
            case YEARS:
                return startDate.plusYears(duration.getValue());
            default:
                return startDate.plusDays(duration.getValue());
        }
    }

    private void validateInputs(String input, String errorMessage) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateCreateRequest(CreateTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create template request cannot be null");
        }

        if (request.getScanId() == null || request.getScanId().trim().isEmpty()) {
            throw new IllegalArgumentException("Scan ID is required for template creation");
        }

        // NEW: Validate status is only DRAFT or PUBLISHED
        if (request.getStatus() == null){
            throw new IllegalArgumentException("Template status cannot be empty or null and must be either DRAFT or PUBLISHED.");
        }
        
        if(request.getUiConfig()==null) {
        	throw new IllegalArgumentException("Template uiConfig cannot be empty or null.");
        }
        
//        if(request.getUiConfig().getTypographySettings()==null) {
//        	throw new IllegalArgumentException("Template typography settings cannot be empty or null.");
//        }

        if (request.getStatus() != TemplateStatus.DRAFT && request.getStatus() != TemplateStatus.PUBLISHED) {
            throw new IllegalArgumentException("Template status must be either DRAFT or PUBLISHED. INACTIVE status is not allowed for template creation.");
        }

        if (request.getPrivacyPolicyDocument() != null &&
                !request.getPrivacyPolicyDocument().trim().isEmpty() &&
                !CommonUtil.isValidBase64(request.getPrivacyPolicyDocument())) {
            throw new IllegalArgumentException("Template privacyPolicyDocument must be a valid Base64 encoded value.");
        }

        // Existing validations...
        if (request.getPreferences() != null && request.getPreferences().isEmpty()) {
            throw new IllegalArgumentException("At least one preference must be provided if preferences are specified");
        }

        // Validate supported languages match content map
        if (request.getMultilingual() != null &&
                request.getMultilingual().getSupportedLanguages() != null &&
                request.getMultilingual().getLanguageSpecificContentMap() != null) {

            for (var language : request.getMultilingual().getSupportedLanguages()) {
                if (!request.getMultilingual().getLanguageSpecificContentMap().containsKey(language)) {
                    throw new IllegalArgumentException("Missing content for supported language: " + language);
                }
            }
        }
        
        if(request.getUiConfig().getTypographySettings()!=null) {
        	if(request.getUiConfig().getTypographySettings().isEmpty()) {
        		throw new IllegalArgumentException("typographySettings cannot be empty");
        	}
        	for(Map.Entry<LANGUAGE, LanguageTypographySettings> entry: request.getUiConfig().getTypographySettings().entrySet()) {
        		LanguageTypographySettings currSettings = request.getUiConfig().getTypographySettings().get(entry.getKey());
        		if(currSettings.getFontFile()==null || currSettings.getFontFile().trim().equals("")) {
        			throw new IllegalArgumentException("fontFile, fontSize, fontWeight and fontStyle in typography settings cannot be empty or null.");
        		}else if(!CommonUtil.isValidBase64(currSettings.getFontFile())){
        			throw new IllegalArgumentException("fontFile should be a valid base64 encoded ttf/otf/woff/woff2 file");
        		}else {
        			String base64File = currSettings.getFontFile();
        			byte[] decodedBytes = Base64.getDecoder().decode(base64File);
        	        String mimeType = new Tika().detect(decodedBytes);
        	        System.out.println(mimeType);
        	        if (!ALLOWED_FONT_TYPES.contains(mimeType)) {
        	            throw new IllegalArgumentException("Invalid font file type: allowed types are ttf/otf/woff/woff2");
        	        }
        			
        		}
        		if(currSettings.getFontSize()==null) {
        			throw new IllegalArgumentException("fontFile, fontSize, fontWeight and fontStyle in typography settings cannot be empty or null.");
        		}
        		if(currSettings.getFontWeight()==null) {
        			throw new IllegalArgumentException("fontFile, fontSize, fontWeight and fontStyle in typography settings cannot be empty or null.");
        		}
        		if(currSettings.getFontStyle()==null  || currSettings.getFontStyle().trim().equals("")) {
        			throw new IllegalArgumentException("fontFile, fontSize, fontWeight and fontStyle in typography settings cannot be empty or null.");
        		}
        	}
        }

        log.debug("Create template request validation passed");
    }

    @Transactional
    public UpdateTemplateResponse updateTemplate(String tenantId, String templateId, String businessId, UpdateTemplateRequest updateRequest) throws ConsentException {

        validateInputs(tenantId, "Tenant ID cannot be null or empty");
        validateInputs(templateId, "Template ID cannot be null or empty");
        validateUpdateRequest(updateRequest);

        // Log template update initiated
        auditService.logTemplateUpdateInitiated(tenantId, businessId, templateId);

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            // Step 2: Find current ACTIVE template
            Query activeQuery = new Query(Criteria.where("templateId").is(templateId)
                    .and("templateStatus").is(VersionStatus.ACTIVE));
            ConsentTemplate currentActiveTemplate = tenantMongoTemplate.findOne(activeQuery, ConsentTemplate.class);

            if (currentActiveTemplate == null) {
                throw new IllegalArgumentException("Template not found: " + templateId);
            }

            // Step 3: Create new template record
            ConsentTemplate newTemplate = new ConsentTemplate();

            // Keep same: templateId, businessId, scanId
            newTemplate.setTemplateId(currentActiveTemplate.getTemplateId());
            newTemplate.setBusinessId(businessId);
            newTemplate.setScanId(currentActiveTemplate.getScanId());

            // Set version: current version + 1
            newTemplate.setVersion(currentActiveTemplate.getVersion() + 1);

            // Set templateStatus: ACTIVE
            newTemplate.setTemplateStatus(VersionStatus.ACTIVE);

            // Apply updates from request or keep existing values
            newTemplate.setTemplateName(updateRequest.getTemplateName() != null ?
                    updateRequest.getTemplateName() : currentActiveTemplate.getTemplateName());
            newTemplate.setStatus(updateRequest.getStatus() != null ?
                    updateRequest.getStatus() : currentActiveTemplate.getStatus());
            newTemplate.setMultilingual(updateRequest.getMultilingual() != null ?
                    updateRequest.getMultilingual() : currentActiveTemplate.getMultilingual());
            newTemplate.setUiConfig(updateRequest.getUiConfig() != null ?
                    updateRequest.getUiConfig() : currentActiveTemplate.getUiConfig());
            newTemplate.setPrivacyPolicyDocument(updateRequest.getPrivacyPolicyDocument() != null ?
                    updateRequest.getPrivacyPolicyDocument() : currentActiveTemplate.getPrivacyPolicyDocument());
            newTemplate.setDocumentMeta(updateRequest.getDocumentMeta() != null ?
                    updateRequest.getDocumentMeta() : currentActiveTemplate.getDocumentMeta());
            newTemplate.setPreferences(updateRequest.getPreferences() != null ?
                    updateRequest.getPreferences() : currentActiveTemplate.getPreferences());

            // Validate preferences if they were updated
            if (updateRequest.getPreferences() != null) {
                validateTemplatePurposes(newTemplate.getPreferences(), tenantId);
            }

            // Set timestamps
            newTemplate.setCreatedAt(Instant.now());
            newTemplate.setUpdatedAt(Instant.now());
            newTemplate.setClassName("com.example.scanner.entity.ConsentTemplate");

            // Step 4: Save new template record
            ConsentTemplate savedNewTemplate = tenantMongoTemplate.save(newTemplate);

            // Log new version created
            auditService.logNewTemplateVersionCreated(tenantId, businessId, templateId);

            // Step 5: Update old template: templateStatus ACTIVE -> UPDATED
            currentActiveTemplate.setTemplateStatus(VersionStatus.UPDATED);
            currentActiveTemplate.setUpdatedAt(Instant.now());
            tenantMongoTemplate.save(currentActiveTemplate);

            return UpdateTemplateResponse.success(
                    templateId,
                    savedNewTemplate.getId(),
                    savedNewTemplate.getVersion(),
                    currentActiveTemplate.getVersion()
            );

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Get template history (all versions) for a logical template ID
     *
     * @param tenantId The tenant identifier
     * @param templateId Logical template ID
     * @return List of all template versions ordered by version DESC (newest first)
     */
    public List<ConsentTemplate> getTemplateHistory(String tenantId, String templateId) {
        validateInputs(tenantId, "Tenant ID cannot be null or empty");
        validateInputs(templateId, "Template ID cannot be null or empty");

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            Query query = new Query(Criteria.where("templateId").is(templateId))
                    .with(Sort.by(Sort.Direction.DESC, "version"));

            List<ConsentTemplate> history = tenantMongoTemplate.find(query, ConsentTemplate.class);

            if (history.isEmpty()) {
                log.warn("No template versions found");
                throw new IllegalArgumentException("Template with ID '" + templateId + "' not found");
            }

            return history;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving template history");
            throw new RuntimeException("Failed to retrieve template history: " + e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }


    /**
     * Get specific version of a template
     * Used when consent handles reference specific template versions
     */
    public Optional<ConsentTemplate> getTemplateByIdAndVersion(String tenantId, String templateId, Integer version) {
        validateInputs(tenantId, "Tenant ID cannot be null or empty");
        validateInputs(templateId, "Template ID cannot be null or empty");

        if (version == null || version <= 0) {
            throw new IllegalArgumentException("Version must be a positive integer");
        }

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            Query query = new Query(Criteria.where("templateId").is(templateId)
                    .and("version").is(version));

            ConsentTemplate template = tenantMongoTemplate.findOne(query, ConsentTemplate.class);
            return Optional.ofNullable(template);

        } finally {
            TenantContext.clear();
        }
    }

    private void validateTemplatePurposes(List<Preference> preferences, String tenantId) throws ConsentException {
        // Empty preferences list is allowed
        if (preferences == null || preferences.isEmpty()) {
            log.debug("PUBLISHED template has no preferences - allowed");
            return;
        }

        for (Preference preference : preferences) {
            try {
                if (preference.getPurpose() == null || preference.getPurpose().trim().isEmpty()) {
                    throw new ConsentException(
                            ErrorCodes.INVALID_TEMPLATE,
                            ErrorCodes.getDescription(ErrorCodes.INVALID_TEMPLATE),
                            "Each preference in PUBLISHED template must have at least one purpose"
                    );
                }

                if (!categoryService.categoryExists(preference.getPurpose(), tenantId)) {
                    throw new ConsentException(
                            ErrorCodes.INVALID_TEMPLATE,
                            ErrorCodes.getDescription(ErrorCodes.INVALID_TEMPLATE),
                            "Category '" + preference.getPurpose() + "' does not exist in the Category table for this tenant"
                    );
                }

                // Validate isMandatory is set
                if (preference.getIsMandatory() == null) {
                    throw new ConsentException(
                            ErrorCodes.INVALID_TEMPLATE,
                            ErrorCodes.getDescription(ErrorCodes.INVALID_TEMPLATE),
                            "isMandatory field is required for all preferences in PUBLISHED template"
                    );
                }

                log.debug("Preference validation passed");

            } catch (ConsentException e) {
                // Re-throw ConsentException as-is
                log.error("Template preference validation failed");
                throw e;
            } catch (Exception e) {
                // Convert any other exception to ConsentException
                log.error("Unexpected error during preference validation");
                throw new ConsentException(
                        ErrorCodes.INVALID_TEMPLATE,
                        ErrorCodes.getDescription(ErrorCodes.INVALID_TEMPLATE),
                        "Error validating template preferences: " + e.getMessage()
                );
            }
        }

        log.debug("All preferences validated successfully");
    }

    private void validateUpdateRequest(UpdateTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Update template request cannot be null");
        }

        // Validate at least one field is being updated
        if (!request.hasUpdates()) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        // Validate status if provided
        if (request.getStatus() != null &&
                request.getStatus() != TemplateStatus.DRAFT &&
                request.getStatus() != TemplateStatus.PUBLISHED) {
            throw new IllegalArgumentException("Template status must be either DRAFT or PUBLISHED");
        }

        // Validate base64 only if privacyPolicyDocument is being updated (not null/empty)
        if (request.getPrivacyPolicyDocument() != null &&
                !request.getPrivacyPolicyDocument().trim().isEmpty() &&
                !CommonUtil.isValidBase64(request.getPrivacyPolicyDocument())) {
            throw new IllegalArgumentException("Template privacyPolicyDocument must be a valid Base64 encoded value.");
        }
        
        if(request.getUiConfig()!=null && request.getUiConfig().getTypographySettings()!=null) {
        	if(request.getUiConfig().getTypographySettings().isEmpty()) {
        		throw new IllegalArgumentException("typographySettings cannot be empty.");
        	}
        	for(Map.Entry<LANGUAGE, LanguageTypographySettings> entry: request.getUiConfig().getTypographySettings().entrySet()) {
        		LanguageTypographySettings currSettings = request.getUiConfig().getTypographySettings().get(entry.getKey());
        		if(currSettings.getFontFile()==null || currSettings.getFontFile().trim().equals("")) {
        			throw new IllegalArgumentException("fontFile, fontSize, fontWeight and fontStyle in typography settings cannot be empty or null.");
        		}else if(!CommonUtil.isValidBase64(currSettings.getFontFile())){
        			throw new IllegalArgumentException("fontFile should be a valid base64 encoded ttf/otf/woff/woff2 file");
        		}else {
        			String base64File = currSettings.getFontFile();
        			byte[] decodedBytes = Base64.getDecoder().decode(base64File);
        	        String mimeType = new Tika().detect(decodedBytes);
        	        System.out.println(mimeType);
        	        if (!ALLOWED_FONT_TYPES.contains(mimeType)) {
        	            throw new IllegalArgumentException("Invalid font file type: allowed types are ttf/otf/woff/woff2");
        	        }
        			
        		}
        		if(currSettings.getFontSize()==null) {
        			throw new IllegalArgumentException("fontFile, fontSize, fontWeight and fontStyle in typography settings cannot be empty or null.");
        		}
        		if(currSettings.getFontWeight()==null) {
        			throw new IllegalArgumentException("fontFile, fontSize, fontWeight and fontStyle in typography settings cannot be empty or null.");
        		}
        		if(currSettings.getFontStyle()==null  || currSettings.getFontStyle().trim().equals("")) {
        			throw new IllegalArgumentException("fontFile, fontSize, fontWeight and fontStyle in typography settings cannot be empty or null.");
        		}
        	}
        }

        log.debug("Update template request validation passed");
    }


    public List<TemplateWithCookiesResponse> getTemplateWithCookies(
            String tenantId, String businessId, String scanId, String templateId) {

        log.info("Fetching template with cookies for tenantId: {}, businessId: {}, scanId: {}, templateId: {}",
                tenantId, businessId, scanId, templateId);

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            // Build dynamic query - always filter by ACTIVE templates
            Query query = new Query();
            query.addCriteria(Criteria.where("templateStatus").is("ACTIVE"));

            // Add businessId filter if provided
            if (businessId != null && !businessId.trim().isEmpty()) {
                query.addCriteria(Criteria.where("businessId").is(businessId));
            }

            // Add scanId filter if provided
            if (scanId != null && !scanId.trim().isEmpty()) {
                query.addCriteria(Criteria.where("scanId").is(scanId));
            }

            // Add templateId filter if provided
            if (templateId != null && !templateId.trim().isEmpty()) {
                query.addCriteria(Criteria.where("templateId").is(templateId));
            }

            query.with(Sort.by(Sort.Direction.DESC, "version"));

            // Execute query
            List<ConsentTemplate> templates = mongoTemplate.find(query, ConsentTemplate.class);

            log.info("Found {} templates matching the criteria", templates.size());

            // Map templates to response with cookies
            return templates.stream()
                    .map(template -> {
                        List<PreferenceWithCookies> preferencesWithCookies =
                                fetchAndMapCookies(template.getScanId(), template.getPreferences(), mongoTemplate);

                        return TemplateWithCookiesResponse.builder()
                                .id(template.getId())
                                .templateId(template.getTemplateId())
                                .scanId(template.getScanId())
                                .templateName(template.getTemplateName())
                                .businessId(template.getBusinessId())
                                .status(template.getStatus())
                                .templateStatus(template.getTemplateStatus())
                                .multilingual(template.getMultilingual())
                                .uiConfig(template.getUiConfig())
                                .documentMeta(template.getDocumentMeta())
                                .privacyPolicyDocument(template.getPrivacyPolicyDocument())
                                .preferencesWithCookies(preferencesWithCookies)
                                .version(template.getVersion())
                                .createdAt(template.getCreatedAt())
                                .updatedAt(template.getUpdatedAt())
                                .build();
                    })
                    .collect(Collectors.toList());

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Fetch cookies from scan and map to preferences
     */
    private List<PreferenceWithCookies> fetchAndMapCookies(
            String scanId, List<Preference> preferences, MongoTemplate mongoTemplate) {

        if (scanId == null || preferences == null || preferences.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Get scan result
            Query scanQuery = new Query(Criteria.where("transactionId").is(scanId));
            ScanResultEntity scanResult = mongoTemplate.findOne(scanQuery, ScanResultEntity.class);

            if (scanResult == null || scanResult.getCookiesBySubdomain() == null) {
                log.warn("No cookies found for scanId");
                // Return preferences without cookies
                return preferences.stream()
                        .map(pref -> PreferenceWithCookies.from(pref, Collections.emptyList()))
                        .collect(Collectors.toList());
            }

            // Flatten all cookies from all subdomains
            List<CookieEntity> allCookies = scanResult.getCookiesBySubdomain().values().stream()
                    .flatMap(List::stream).toList();

            // Group cookies by category
            Map<String, List<CookieEntity>> cookiesByCategory = allCookies.stream()
                    .filter(cookie -> cookie.getCategory() != null)
                    .collect(Collectors.groupingBy(CookieEntity::getCategory));

            // Map cookies to preferences based on purpose
            return preferences.stream()
                    .map(preference -> {
                        String categoryKey = preference.getPurpose();
                        List<CookieEntity> matchingCookies = cookiesByCategory.getOrDefault(
                                categoryKey, Collections.emptyList());

                        return PreferenceWithCookies.from(preference, matchingCookies);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching cookies");
            // Return preferences without cookies on error
            return preferences.stream()
                    .map(pref -> PreferenceWithCookies.from(pref, Collections.emptyList()))
                    .collect(Collectors.toList());
        }
    }

    public Optional<ConsentTemplate> getLatestPublishedTemplate(String tenantId, String templateId) {
        validateInputs(tenantId, "Tenant ID cannot be null or empty");
        validateInputs(templateId, "Template ID cannot be null or empty");

        TenantContext.setCurrentTenant(tenantId);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        try {
            // Query for PUBLISHED status and ACTIVE templateStatus, sorted by version descending
            Query query = new Query(Criteria.where("templateId").is(templateId)
                    .and("status").is("PUBLISHED")
                    .and("templateStatus").is("ACTIVE"))
                    .with(Sort.by(Sort.Direction.DESC, "version"))
                    .limit(1);

            ConsentTemplate template = tenantMongoTemplate.findOne(query, ConsentTemplate.class);

            if (template != null) {
                log.info("Latest published template found for templateId: {}, version: {}",
                        templateId, template.getVersion());
            } else {
                log.warn("No published and active template found for templateId: {}", templateId);
            }

            return Optional.ofNullable(template);

        } catch (Exception e) {
            log.error("Error retrieving latest published template for templateId: {}", templateId);
            throw new RuntimeException("Failed to retrieve latest published template: " + e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}