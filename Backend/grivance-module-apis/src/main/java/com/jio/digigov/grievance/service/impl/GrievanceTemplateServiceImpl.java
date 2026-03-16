package com.jio.digigov.grievance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.grievance.config.MultiTenantMongoConfig;
import com.jio.digigov.grievance.config.NotificationConfig;
import com.jio.digigov.grievance.dto.LANGUAGE;
import com.jio.digigov.grievance.dto.LanguageTypographySettings;
import com.jio.digigov.grievance.dto.PagedDetailResponse;
import com.jio.digigov.grievance.dto.request.GrievanceTemplateRequest;
import com.jio.digigov.grievance.entity.GrievanceTemplate;
import com.jio.digigov.grievance.entity.GrievanceType;
import com.jio.digigov.grievance.entity.UserDetail;
import com.jio.digigov.grievance.entity.UserType;
import com.jio.digigov.grievance.enumeration.Status;
import com.jio.digigov.grievance.exception.BusinessException;
import com.jio.digigov.grievance.exception.InvalidBusinessIdException;
import com.jio.digigov.grievance.exception.ValidationException;
import com.jio.digigov.grievance.integration.audit.AuditEventService;
import com.jio.digigov.grievance.mapper.GrievanceTemplateMapper;
import com.jio.digigov.grievance.service.GrievanceTemplateService;
import com.jio.digigov.grievance.util.GrievanceQueryUtils;
import com.jio.digigov.grievance.util.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.tika.Tika;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrievanceTemplateServiceImpl implements GrievanceTemplateService {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    private NotificationConfig notificationConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AuditEventService auditEventService;
    
    private static final Set<String> ALLOWED_FONT_TYPES = Set.of(
            "font/ttf",
            "font/otf",
            "font/woff",
            "font/woff2",
            "application/x-font-ttf",
            "application/x-font-otf",
            "application/font-woff"
    );

    @Override
    public GrievanceTemplate create(GrievanceTemplate req, String tenantId, String businessId, String transactionId, HttpServletRequest httpServletRequest) {
        log.info("Creating GrievanceTemplate for tenant={}, business={}", tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            // ----------------- Validation -----------------
            validateGrievanceTemplate(req, tenantId, businessId);

            if (req.getCreatedAt() == null) req.setCreatedAt(LocalDateTime.now());
            req.setBusinessId(businessId);

            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            // --- Unique Name Check ---
            validateUniqueTemplateName(req.getGrievanceTemplateName(), businessId, tenantMongoTemplate);
            GrievanceTemplate saved = tenantMongoTemplate.save(req);

            log.info("Saved GrievanceTemplate {} in tenant_db_{}", saved.getGrievanceTemplateId(), tenantId);

            // Trigger Audit Event
            auditEventService.triggerAuditEventForTemplate(saved, tenantId, businessId, transactionId, httpServletRequest);

            return saved;

        } catch (Exception e) {
            log.error("Failed to create GrievanceTemplate for tenant={}, business={}", tenantId, businessId, e);
            throw new BusinessException("GRIEVANCE_TEMPLATE_CREATION_FAILED", e.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void validateUniqueTemplateName(String name, String businessId, MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("grievanceTemplateName").is(name));
        query.addCriteria(Criteria.where("businessId").is(businessId));

        if (mongoTemplate.exists(query, GrievanceTemplate.class)) {
            throw new BusinessException("TEMPLATE_NAME_ALREADY_EXISTS",
                    "GrievanceTemplateName must be unique");
        }
    }

    /**
     * Validates that the provided grievanceTemplate request values exist in the DB.
     */
    private void validateGrievanceTemplate(GrievanceTemplate req, String tenantId, String businessId) {
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        // ---------------- Validate grievance types and items ----------------
        if (req.getMultilingual() != null && req.getMultilingual().getGrievanceInformation() != null) {
            for (GrievanceTemplate.GrievanceInfo info : req.getMultilingual().getGrievanceInformation()) {
                // Build criteria: grievanceType + (businessId == businessId OR businessId == tenantId)
                Criteria criteria = new Criteria().andOperator(
                        Criteria.where("grievanceType").is(info.getGrievanceType()),
                        new Criteria().orOperator(
                                Criteria.where("businessId").is(businessId),  // business record
                                Criteria.where("businessId").is(tenantId)     // tenant-level default record
                        )
                );

                // Execute query
                GrievanceType dbType = tenantMongoTemplate.findOne(
                        new Query(criteria),
                        GrievanceType.class
                );

                if (dbType == null) {
                    throw new BusinessException(
                            "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                            "Grievance type " + info.getGrievanceType() + " does not exist"
                    );
                }

                for (String item : info.getGrievanceItems()) {
                    if (!dbType.getGrievanceItem().contains(item)) {
                        throw new BusinessException(
                                "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                                "Grievance item " + item + " does not exist for type " + info.getGrievanceType()
                        );
                    }
                }
            }
        }

        // ---------------- Validate user types ----------------
        if (req.getMultilingual() != null && req.getMultilingual().getUserInformation() != null) {
            for (GrievanceTemplate.UserInformation userInfo : req.getMultilingual().getUserInformation()) {

                // Validate each user type
                for (String userType : userInfo.getUserType()) {
                    Criteria criteria = new Criteria().andOperator(
                            Criteria.where("name").is(userType),
                            new Criteria().orOperator(
                                    Criteria.where("businessId").is(businessId),  // business-level
                                    Criteria.where("businessId").is(tenantId)     // tenant-level
                            )
                    );

                    UserType dbUserType = tenantMongoTemplate.findOne(
                            new Query(criteria),
                            UserType.class
                    );

                    if (dbUserType == null) {
                        throw new BusinessException(
                                "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                                "User type " + userType + " does not exist"
                        );
                    }
                }

                // Validate each user detail
                for (String userDetail : userInfo.getUserItems()) {
                    Criteria criteria = new Criteria().andOperator(
                            Criteria.where("name").is(userDetail),
                            new Criteria().orOperator(
                                    Criteria.where("businessId").is(businessId),  // business-level record
                                    Criteria.where("businessId").is(tenantId)     // tenant-level record
                            )
                    );

                    UserDetail dbUserDetail = tenantMongoTemplate.findOne(
                            new Query(criteria),
                            UserDetail.class
                    );
                    if (dbUserDetail == null) {
                        throw new BusinessException(
                                "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                                "User detail " + userDetail + " does not exist"
                        );
                    }
                }
            }
        }
        if(req.getUiConfig()==null) {
        	throw new BusinessException(
                    "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                    "UiConfig should not be empty or null"
            );
        }else if(req.getUiConfig().getTypographySettings()!=null){
        	validateTypographySettings(req.getUiConfig().getTypographySettings());
        }    
    }


    @Override
    public List<GrievanceTemplate> listByBusinessId(String tenantId, String businessId, Integer page, Integer size) {
        log.info("Listing GrievanceTemplates for tenant={}, business={} with page={} size={}",
                tenantId, businessId, page, size);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query query = new Query().addCriteria(Criteria.where("businessId").is(businessId));

            // Apply pagination only when both page & size are provided
            if (page != null && size != null && size > 0) {
                int pageIndex = Math.max(0, page);
                query.skip((long) pageIndex * size).limit(size);
                log.info("Paginated fetch applied | page={} size={}", pageIndex, size);
            } else {
                log.info("No pagination applied | fetching all templates");
            }

            return tenantMongoTemplate.find(query, GrievanceTemplate.class);

        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public Optional<GrievanceTemplate> getById(String templateId, String tenantId, String businessId) {
        log.info("Fetching GrievanceTemplate by ID={} for tenant={}, business={}", templateId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            Query query = new Query().addCriteria(
                    Criteria.where("grievanceTemplateId").is(templateId)
                            .and("businessId").is(businessId)
            );
            query.with(Sort.by(Sort.Direction.DESC, "version"));

            GrievanceTemplate grievanceTemplate = tenantMongoTemplate.findOne(query, GrievanceTemplate.class);

            if (grievanceTemplate == null) {
                log.warn("No GrievanceTemplate found with ID={} for tenant={}, business={}", templateId, tenantId, businessId);
                return Optional.empty();
            }

            log.info("Fetched GrievanceTemplate {} successfully for tenant={}, business={}",
                    grievanceTemplate.getGrievanceTemplateName(), tenantId, businessId);

            return Optional.of(grievanceTemplate);
        } catch (Exception e) {
            log.error("Error fetching GrievanceTemplate by ID={} for tenant={}, business={} | error={}",
                    templateId, tenantId, businessId, e.getMessage(), e);
            throw new BusinessException("TEMPLATE_FETCH_FAILED", e.getMessage());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public List<GrievanceTemplate> search(
            String tenantId,
            String businessId,
            Map<String, String> params,
            Integer page,
            Integer size
    ) {

        log.info("[SEARCH] GrievanceTemplates | tenantId={} | businessId={} | filters={} | page={} | size={}",
                tenantId, businessId, params, page, size);

        // Validate businessId
        GrievanceQueryUtils.validateBusinessId(businessId);

        // Tenant context setup
        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
            // Build dynamic query including nested fields
            Query query = GrievanceQueryUtils.buildQuery(params);
            log.debug("🔍 Mongo Query: {}", query);

            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            List<GrievanceTemplate> results;

            boolean applyPagination = (page != null && size != null && page >= 0 && size > 0);

            if (applyPagination) {
                long skip = (long) page * size;
                query.skip(skip).limit(size);

                log.info("Applying pagination skip={} limit={}", skip, size);
            } else {
                log.info("Pagination NOT applied");
            }

            query.with(Sort.by(Sort.Direction.DESC, "version"));
            results = mongoTemplate.find(query, GrievanceTemplate.class);

            log.info("Returning {} grievance templates", results.size());

            return results;

        } catch (InvalidBusinessIdException ex) {
            log.error("Invalid businessId={} during search", businessId, ex);
            throw ex;

        } catch (Exception ex) {
            log.error("Unexpected error during search | tenantId={} | businessId={}", tenantId, businessId, ex);
            throw new RuntimeException("Failed to search grievance templates", ex);

        } finally {
            TenantContextHolder.clear();
            log.debug("Tenant context cleared after search operation.");
        }
    }

    @Override
    public GrievanceTemplate update(String templateId,
                                    GrievanceTemplateRequest request,
                                    String tenantId,
                                    String businessId,
                                    String transactionId,
                                    HttpServletRequest httpServletRequest) {

        log.info("TXN={} | Updating GrievanceTemplate={} tenant={} business={}",
                transactionId, templateId, tenantId, businessId);

        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);

        try {
//        	GrievanceTemplate entity = GrievanceTemplateMapper.toEntity(request);
//        	validateGrievanceTemplate(entity, tenantId, businessId);
        	if(request.getUiConfig()!=null && request.getUiConfig().getTypographySettings()!=null) {
        		validateTypographySettings(request.getUiConfig().getTypographySettings());
        	}
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // Fetch existing
            Query query = new Query().addCriteria(
                    Criteria.where("grievanceTemplateId").is(templateId)
                            .and("businessId").is(businessId)
            );
            query.with(Sort.by(Sort.Direction.DESC, "version"));

            GrievanceTemplate existing = mongoTemplate.findOne(query, GrievanceTemplate.class);

            if (existing == null) {
                log.warn("TXN={} | Template not found for ID={}", transactionId, templateId);
                throw new BusinessException("TEMPLATE_NOT_FOUND",
                        "No grievance template found for ID: " + templateId);
            }

            log.info("TXN={} | Existing template found name={} status={}",
                    transactionId, existing.getGrievanceTemplateName(), existing.getStatus());

            boolean isPublishing = request.getStatus() != null
                    && "PUBLISHED".equalsIgnoreCase(String.valueOf(request.getStatus()));

            // ----------------------------------------------------------------------
            // CASE 1: If request is PUBLISHED -> Create a NEW VERSION
            // ----------------------------------------------------------------------
            if (isPublishing) {

                int latestVersion = getLatestVersionGrievanceTemplate(businessId, templateId);

                GrievanceTemplate newVersion = new GrievanceTemplate();
                BeanUtils.copyProperties(existing, newVersion);

                newVersion.setId(null);
                newVersion.setVersion(latestVersion + 1);
                newVersion.setStatus(Status.PUBLISHED);
                newVersion.setCreatedAt(LocalDateTime.now());
                newVersion.setUpdatedAt(LocalDateTime.now());

                // PATCH update fields
                applyPartialUpdate(newVersion, request);

                // Check duplicate name only if modified
                if (request.getGrievanceTemplateName() != null &&
                        !request.getGrievanceTemplateName()
                                .equalsIgnoreCase(existing.getGrievanceTemplateName())) {

                    validateUniqueTemplateName(newVersion.getGrievanceTemplateName(),
                            businessId, mongoTemplate);
                }

                mongoTemplate.save(newVersion);

                log.info("TXN={} | New version={} created for template={}",
                        transactionId, newVersion.getVersion(), newVersion.getGrievanceTemplateName());

                auditEventService.triggerAuditEventForTemplate(existing, tenantId, businessId,
                        transactionId, httpServletRequest);

                return newVersion;
            }

            // ----------------------------------------------------------------------
            // CASE 2: If NOT PUBLISHED -> Update SAME document
            // ----------------------------------------------------------------------
            else {

                String oldName = existing.getGrievanceTemplateName();
                String newName = request.getGrievanceTemplateName();

                boolean nameChanged =
                        newName != null && !newName.equalsIgnoreCase(oldName);

                // Patch update fields
                applyPartialUpdate(existing, request);

                existing.setUpdatedAt(LocalDateTime.now());

                // Unique name check ONLY if name changed
                if (nameChanged) {
                    validateUniqueTemplateName(existing.getGrievanceTemplateName(),
                            businessId, mongoTemplate);
                }

                mongoTemplate.save(existing);

                log.info("TXN={} | Template={} updated successfully",
                        transactionId, templateId);

                auditEventService.triggerAuditEventForTemplate(existing, tenantId,
                        businessId, transactionId, httpServletRequest);

                return existing;
            }

        } catch (Exception e) {

            log.error("TXN={} | Failed to update GrievanceTemplate={} error={}",
                    transactionId, templateId, e.getMessage(), e);

            throw new BusinessException("TEMPLATE_UPDATE_FAILED", e.getMessage());

        } finally {
            TenantContextHolder.clear();
        }
    }


    private void applyPartialUpdate(
            GrievanceTemplate target,
            GrievanceTemplateRequest req
    ) {

        // 1. Name
        if (req.getGrievanceTemplateName() != null)
            target.setGrievanceTemplateName(req.getGrievanceTemplateName());

        // 2. Status
        if (req.getStatus() != null)
            target.setStatus(req.getStatus());


        // -------------------------------------------------
        // 3. Multilingual (with FIXED mapping)
        // -------------------------------------------------
        if (req.getMultilingual() != null) {

            if (target.getMultilingual() == null)
                target.setMultilingual(new GrievanceTemplate.Multilingual());

            var m = target.getMultilingual();
            var r = req.getMultilingual();

            if (r.getEnabled() != null) m.setEnabled(r.getEnabled());
            if (r.getSupportedLanguages() != null) m.setSupportedLanguages(r.getSupportedLanguages());
            if (r.getDescriptionCheck() != null) m.setDescriptionCheck(r.getDescriptionCheck());
            if (r.getUploadFiles() != null) m.setUploadFiles(r.getUploadFiles());

            // --------- FIX 1: Map grievanceInformation (DTO → Entity) ----------
            if (r.getGrievanceInformation() != null) {

                List<GrievanceTemplate.GrievanceInfo> mappedGrievanceInfo =
                        r.getGrievanceInformation().stream()
                                .map(g -> GrievanceTemplate.GrievanceInfo.builder()
                                        .grievanceType(g.getGrievanceType())
                                        .grievanceItems(g.getGrievanceItems())
                                        .build())
                                .toList();

                m.setGrievanceInformation(mappedGrievanceInfo);
            }

            // --------- FIX 2: Map userInformation (DTO → Entity) ----------
            if (r.getUserInformation() != null) {

                List<GrievanceTemplate.UserInformation> mappedUserInfo =
                        r.getUserInformation().stream()
                                .map(u -> GrievanceTemplate.UserInformation.builder()
                                        .userType(u.getUserType())
                                        .userItems(u.getUserItems())
                                        .build())
                                .toList();

                m.setUserInformation(mappedUserInfo);
            }
        }


        // -------------------------------------------------
        // 4. UiConfig
        // -------------------------------------------------
        if (req.getUiConfig() != null) {

            if (target.getUiConfig() == null)
                target.setUiConfig(new GrievanceTemplate.UiConfig());

            var ui = target.getUiConfig();
            var r = req.getUiConfig();

            if (r.getLogo() != null) ui.setLogo(r.getLogo());
            if (r.getTheme() != null) ui.setTheme(r.getTheme());
            if (r.getLogoName() != null) ui.setLogoName(r.getLogoName());
            if (r.getDarkMode() != null) ui.setDarkMode(r.getDarkMode());
            if (r.getMobileView() != null) ui.setMobileView(r.getMobileView());
            if (r.getTypographySettings() != null) ui.setTypographySettings(r.getTypographySettings());
        }


        // -------------------------------------------------
        // 5. Languages Map (PATCH behavior + DTO→Entity FIX)
        // -------------------------------------------------
        if (req.getLanguages() != null && !req.getLanguages().isEmpty()) {

            if (target.getLanguages() == null)
                target.setLanguages(new HashMap<>());

            req.getLanguages().forEach((langKey, reqLangValue) -> {

                var existingLang = target.getLanguages().get(langKey);

                if (existingLang == null) {

                    // FIXED: Convert DTO → ENTITY
                    GrievanceTemplate.LanguageContent newLang =
                            GrievanceTemplate.LanguageContent.builder()
                                    .heading(reqLangValue.getHeading())
                                    .description(reqLangValue.getDescription())
                                    .build();

                    target.getLanguages().put(langKey, newLang);

                } else {

                    // PATCH update
                    if (reqLangValue.getHeading() != null)
                        existingLang.setHeading(reqLangValue.getHeading());

                    if (reqLangValue.getDescription() != null)
                        existingLang.setDescription(reqLangValue.getDescription());
                }
            });
        }
    }

    private int getLatestVersionGrievanceTemplate(String businessId, String grievanceTemplateId) {
        MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(TenantContextHolder.getTenant());
        Query query = new Query().addCriteria(
                Criteria.where("businessId").is(businessId)
                        .and("grievanceTemplateId").is(grievanceTemplateId)
        );
        query.with(Sort.by(Sort.Direction.DESC, "version"));
        GrievanceTemplate latest = mongoTemplate.findOne(query, GrievanceTemplate.class);
        return latest != null ? latest.getVersion() : 0;
    }


    @Override
    public boolean delete(String templateId, String tenantId, String businessId) {
        log.info("Deleting GrievanceTemplate {} for tenant={}, business={}", templateId, tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query q = new Query().addCriteria(
                    Criteria.where("grievanceTemplateId").is(templateId)
                            .and("businessId").is(businessId)
            );
            GrievanceTemplate existing = tenantMongoTemplate.findOne(q, GrievanceTemplate.class);
            if (existing == null) return false;

            tenantMongoTemplate.remove(q, GrievanceTemplate.class);
            log.info("Deleted GrievanceTemplate {} from tenant_db_{}", templateId, tenantId);
            return true;
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public long count(String tenantId, String businessId) {
        log.info("Counting GrievanceTemplates for tenant={}, business={}", tenantId, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setTenant(tenantId);
        TenantContextHolder.setBusinessId(businessId);
        try {
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            Query q = new Query().addCriteria(Criteria.where("businessId").is(businessId));
            return tenantMongoTemplate.count(q, GrievanceTemplate.class);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public int getLatestVersion(String businessId, String grievanceTemplateName) {
        log.info("Fetching latest version for template={} business={}", grievanceTemplateName, businessId);
        validateBusinessId(businessId);

        TenantContextHolder.setBusinessId(businessId);
        try {
            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(TenantContextHolder.getTenant());
            Query q = new Query().addCriteria(
                    Criteria.where("businessId").is(businessId)
                            .and("grievanceTemplateName").is(grievanceTemplateName)
            );
            q.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "version"));
            GrievanceTemplate latest = mongoTemplate.findOne(q, GrievanceTemplate.class);
            return latest != null ? latest.getVersion() : 0;
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void validateBusinessId(String businessId) {
        if (businessId == null || businessId.trim().isEmpty()) {
            throw new ValidationException("X-Business-Id header is required for grievance-template operations");
        }
    }
    
    public static boolean isValidBase64(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        String trimmed = str.trim();

        if (trimmed.length() % 4 != 0) {
            return false;
        }

        if (!trimmed.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            return false;
        }

        try {
            Base64.getDecoder().decode(trimmed);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public void validateTypographySettings(Map<LANGUAGE, LanguageTypographySettings> typographySettings) {
    	if(typographySettings.isEmpty()) {
    		throw new BusinessException(
                    "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                    "typography settings cannot be empty."
            );
    	}
    	for(Map.Entry<LANGUAGE, LanguageTypographySettings> entry: typographySettings.entrySet()) {
    		LanguageTypographySettings currSettings = typographySettings.get(entry.getKey());
    		if(currSettings.getFontFile()==null || currSettings.getFontFile().trim().isEmpty()) {
    			throw new BusinessException(
                        "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                        "fontFile in typography settings cannot be empty or null."
                );
    		}else if(isValidBase64(currSettings.getFontFile())){
    			try {
    				String base64File = currSettings.getFontFile();
        			byte[] decodedBytes = Base64.getDecoder().decode(base64File);
        	        String mimeType = new Tika().detect(decodedBytes);
        	        if (!ALLOWED_FONT_TYPES.contains(mimeType)) {
        	        	throw new BusinessException(
                                "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                                "Invalid font file type: font file should be a valid base64 encoded ttf/otf/woff/woff2."
                        );
        	        }
    	        } catch (IllegalArgumentException e) {
    	        	throw new BusinessException(
                            "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                            "Invalid font file type: font file should be a valid base64 encoded ttf/otf/woff/woff2."
                    );
    	        }
    		}else {
    			throw new BusinessException(
                        "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                        "Invalid font file type: font file should be a valid base64 encoded ttf/otf/woff/woff2."
                );
    		}
    		if(currSettings.getFontSize()==null) {
    			throw new BusinessException(
                        "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                        "fontSize in typography settings cannot be empty or null."
                );
    		}
    		if(currSettings.getFontWeight()==null) {
    			throw new BusinessException(
                        "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                        "fontWeight in typography settings cannot be empty or null."
                );
    		}
    		if(currSettings.getFontStyle()==null) {
    			throw new BusinessException(
                        "GRIEVANCE_TEMPLATE_CREATION_FAILED",
                        "fontStyle in typography settings cannot be empty or null."
                );
    		}
    	}
    }
}
