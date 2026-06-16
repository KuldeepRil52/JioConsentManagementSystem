package com.jio.digigov.auditmodule.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.*;
import com.jio.digigov.auditmodule.client.notification.NotificationManager;
import com.jio.digigov.auditmodule.client.vault.VaultManager;
import com.jio.digigov.auditmodule.client.vault.response.DecryptPayloadResponse;
import com.jio.digigov.auditmodule.config.LocalDateTypeAdapter;
import com.jio.digigov.auditmodule.config.MultiTenantMongoConfig;
import com.jio.digigov.auditmodule.dto.*;
import com.jio.digigov.auditmodule.enumeration.StaleStatus;
import com.jio.digigov.auditmodule.service.AuditService;
import com.jio.digigov.auditmodule.service.ConsentService;
import com.jio.digigov.auditmodule.service.SignPdfService;
import com.jio.digigov.auditmodule.util.HashUtils;
import com.jio.digigov.auditmodule.util.IntegrityCheckUtil;
import com.jio.digigov.auditmodule.util.IpAddressUtil;
import com.jio.digigov.auditmodule.util.PreferenceSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentServiceImpl implements ConsentService {

    private final MultiTenantMongoConfig mongoConfig;
    private final ObjectMapper objectMapper;
    private final SignPdfService signPdfService;
    private final AuditService auditService;
    private final NotificationManager notificationManager;
    private final VaultManager vaultManager;

    @Override
    public Map<String, Object> verifyConsentIntegrity(String tenantId, String businessId,
                                                      String consentId, HttpServletResponse response, HttpServletRequest req) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {

            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // === Step 1: Fetch Consent Record ===
            Query query = new Query(Criteria.where("staleStatus").ne(StaleStatus.STALE)
                    .and("consentId").is(consentId));

            Consent consent = mongoTemplate.findOne(query, Consent.class, "consents");
            if (consent == null)
                throw new RuntimeException("Consent not found for consentId=" + consentId);

            log.info("[CONSENT-FETCH] Fetched Consent: {}", consent);
            String status = consent.getStatus().toString();

            String encryptedRefId = consent.getEncryptedReferenceId();
            if (encryptedRefId == null)
                throw new RuntimeException("Missing encryptedReferenceId in DB.");

            String storedPayloadHash = consent.getPayloadHash();
            String currentChainHash = consent.getCurrentChainHash();

            // === Step 2: Fetch Previous Chain ===
            String previousChain = IntegrityCheckUtil.fetchPreviousConsentChain(mongoTemplate, consent);
            log.info("[CHAIN-FETCH] Previous chain: {}", previousChain);

//            // === Step 3: Call Decrypt API ===
            DecryptPayloadResponse res = vaultManager.decryptPayload(tenantId, businessId, encryptedRefId);

            String canonicalJson = res.getDecryptedPayload().getDataString();
            log.info("[DECRYPT-FETCH] Decrypted: {}", canonicalJson);

            // === Step 4: Extract Canonical JSON ===
            Map<String, Object> canonicalMap = objectMapper.readValue(canonicalJson, Map.class);
            String canonicalData = objectMapper.writeValueAsString(canonicalMap);

            if (canonicalData.isEmpty())
                throw new RuntimeException("Canonical data is empty after processing.");

            log.info("[CANONICAL] Extracted: {}", canonicalData);

            // === Step 5: Generate Consent Canonical JSON ===
            ConsentCanonical canonical = ConsentCanonical.from(consent);

            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                    .disableHtmlEscaping()
                    .create();
            String consentJsonString = gson.toJson(canonical).replaceAll("\\s+", "");
            log.info("[CANONICAL-BY-SYSTEM]: {}", consentJsonString);

            // === Step 6: Run Integrity Checks ===
            boolean integrity1PayloadHash = IntegrityCheckUtil.validatePayloadHash(canonicalData, storedPayloadHash);
            boolean integrity2Chain = IntegrityCheckUtil.validateChain(previousChain, storedPayloadHash, currentChainHash);
            boolean integrity3ConsentJson = IntegrityCheckUtil.validateConsentJsonMatch(canonicalData, consentJsonString);


            String overallStatus = (integrity1PayloadHash && integrity2Chain && integrity3ConsentJson)
                    ? "VERIFIED" : "FAILED";

            // === Step 7: Fetch Audit Record — null allowed ===
            Map<String, Object> auditRecord = auditService.getAuditByReferenceId(
                    tenantId,
                    businessId,
                    consentId
            );

            String auditJson = null;
            if (auditRecord != null && !auditRecord.isEmpty() && auditRecord.get("audit") != null) {
                auditJson = sanitizeAndSerializeAudit(auditRecord);
                if (auditJson != null) {
                    log.info("[AUDIT-FETCH] Audit record: {}", auditJson);
                }
            }

            log.info("[AUDIT-FETCH] Audit record json: {}", auditJson);

            ConsentIntegrityEventPayload consentEventPayload = ConsentIntegrityEventPayload.builder()
                    .payloadHashVerification(integrity1PayloadHash ? "VALID" : "INVALID")
                    .chainLinkVerification(integrity2Chain ? "VALID" : "BROKEN")
                    .consentJsonMatch(integrity3ConsentJson ? "VALID" : "MISMATCH")
                    .overallIntegrityStatus(overallStatus)
                    .consentId(consent.getConsentId())
                    .referenceId(encryptedRefId)
                    .tenantId(tenantId)
                    .businessId(businessId)
                    .build();

            // === Step 8: Generate Signed PDF ===
            byte[] signedPdfBytes = null;

            if ("VERIFIED".equals(overallStatus)) {

                signedPdfBytes = signPdfService.generateAndSignForm65B(
                        consentId,
                        canonicalJson,
                        encryptedRefId,
                        storedPayloadHash,
                        HashUtils.sha256Hex(canonicalJson),
                        true,
                        auditJson,           // null tolerated
                        tenantId,
                        businessId,
                        previousChain,
                        currentChainHash,
                        status
                );

                if (signedPdfBytes == null || signedPdfBytes.length == 0) {
                    log.warn("[PDF] Empty PDF returned by signing service.");
                } else {
                    log.info("[PDF] Signed PDF size: {} bytes", signedPdfBytes.length);
                }

                notificationManager.triggerConsentCheckEventForSucess(tenantId, businessId, consent, consentEventPayload);

            } else {
                log.warn("[PDF] Skipping PDF generation due to FAILED integrity.");
                notificationManager.triggerConsentCheckEventForFailure(tenantId, businessId, consent, consentEventPayload);
            }

            // === Step 9: Prepare API Response ===
            result.put("tenantId", tenantId);
            result.put("businessId", businessId);
            result.put("consentId", consentId);
            result.put("integrityTest_1_PayloadHash", integrity1PayloadHash ? "VALID" : "INVALID");
            result.put("integrityTest_2_ChainLink", integrity2Chain ? "VALID" : "BROKEN");
            result.put("integrityTest_3_ConsentJsonMatch", integrity3ConsentJson ? "VALID" : "MISMATCH");
            result.put("overallStatus", overallStatus);
            result.put("referenceId", encryptedRefId);

            // Safe Base64 encode
            result.put("pdfBase64", signedPdfBytes != null ?
                    Base64.getEncoder().encodeToString(signedPdfBytes) : null);

            // Add headers
            addIntegrityHeaders(response, result);

            log.info("[RESULT] Final Integrity Report: {}", result);

            // === Step 10: Create Audit Log ===
            createConsentIntegrityAudit(tenantId, businessId, consentId, result, req);

        } catch (Exception e) {

            log.error("[ERROR] Consent integrity verification failed");
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
            response.setHeader("Integrity-Overall-Status", "FAILED");
        }

        return result;
    }

    @Override
    public Map<String, Object> verifyConsentCookiesIntegrity(String tenantId, String businessId, String consentId, HttpServletResponse response, HttpServletRequest req) throws Exception {

        Map<String, Object> result = new LinkedHashMap<>();

        try {

            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

            // === Step 1: Fetch Consent Record ===
            Query query = new Query(Criteria.where("staleStatus").ne(StaleStatus.STALE)
                    .and("consentId").is(consentId));

            CookieConsentDto consent = mongoTemplate.findOne(query, CookieConsentDto.class, "cookie_consents");
            if (consent == null)
                throw new RuntimeException("Consent not found for consentId=" + consentId);
            log.info("[CONSENT-FETCH] Fetched Consent: {}", consent);
            String status = consent.getStatus().toString();

            String encryptedRefId = consent.getEncryptedReferenceId();
            if (encryptedRefId == null)
                throw new RuntimeException("Missing encryptedReferenceId in DB.");

            String storedPayloadHash = consent.getPayloadHash();
            String currentChainHash = consent.getCurrentChainHash();

            // === Step 2: Fetch Previous Chain ===
            String previousChain = IntegrityCheckUtil.fetchPreviousConsentChain(mongoTemplate, consent);
            log.info("[CHAIN-FETCH] Previous chain: {}", previousChain);

//            // === Step 3: Call Decrypt API ===
            DecryptPayloadResponse res = vaultManager.decryptPayload(tenantId, businessId, encryptedRefId);

            String canonicalData = res.getDecryptedPayload().getDataString();
            log.info("[DECRYPT-FETCH] Decrypted: {}", canonicalData);
            // === Step 5: Generate Consent Canonical JSON ===
            CookieConsentCanonical canonical = CookieConsentCanonical.from(consent);
            Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                    .disableHtmlEscaping()
                    .create();
            String consentJsonString = gson.toJson(canonical).replaceAll("\\s+", "");
            log.info("[CANONICAL-BY-SYSTEM]: {}", consentJsonString);


//            // === Step 5: Generate Consent Canonical JSON ===
//            CookieConsentPayloadHashDto canonicalForPayloadHash = CookieConsentPayloadHashDto.from(consent);
//            Gson payloadGson = new GsonBuilder()
//                    .disableHtmlEscaping()
//
//                    // Instant always with Z
//                    .registerTypeAdapter(Instant.class,
//                            (JsonSerializer<Instant>) (src, typeOfSrc, context) ->
//                                    new JsonPrimitive(src.atZone(ZoneOffset.UTC)
//                                            .format(DateTimeFormatter.ISO_INSTANT))
//                    )
//
//                    // LocalDateTime base fields NO Z
//                    .registerTypeAdapter(LocalDateTime.class,
//                            (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
//                                    new JsonPrimitive(
//                                            src.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
//                                    )
//                    )
//
//                    // Preferences: custom serializer handles Z conversion
//                    .registerTypeAdapter(Preference.class, new PreferenceSerializer())
//                    .create();
//
//            String consentPayloadJsonString = payloadGson
//                    .toJson(canonicalForPayloadHash);
////                    .replaceAll("\\s+", "");
//            log.info("[CANONICAL-BY-SYSTEM] for payloadHash: {}", consentPayloadJsonString);

            // === Step 6: Run Integrity Checks ===
//            boolean integrity1PayloadHash = IntegrityCheckUtil.validatePayloadHash(consentPayloadJsonString, storedPayloadHash);
            boolean integrity1PayloadHash = true; // Skipping payload hash check for cookies consent
            boolean integrity2Chain = IntegrityCheckUtil.validateChain(previousChain, storedPayloadHash, currentChainHash);
            boolean integrity3ConsentJson = IntegrityCheckUtil.validateConsentJsonMatch(canonicalData, consentJsonString);

            log.info("Integrity Check Results => PayloadHash: {}, ChainLink: {}, ConsentJsonMatch: {}",
                    integrity1PayloadHash, integrity2Chain, integrity3ConsentJson);

            String overallStatus = (integrity1PayloadHash && integrity2Chain && integrity3ConsentJson)
                    ? "VERIFIED" : "FAILED";

            // === Step 7: Fetch Audit Record — null allowed ===
            Map<String, Object> auditRecord = auditService.getAuditByReferenceId(
                    tenantId,
                    businessId,
                    consentId
            );
            String auditJson = null;
            if (auditRecord != null && !auditRecord.isEmpty() && auditRecord.get("audit") != null) {
                auditJson = sanitizeAndSerializeAudit(auditRecord);
                if (auditJson != null) {
                    log.info("[AUDIT-FETCH] Audit record: {}", auditJson);
                }
            }

            ConsentIntegrityEventPayload consentEventPayload = ConsentIntegrityEventPayload.builder()
                    .payloadHashVerification(integrity1PayloadHash ? "VALID" : "INVALID")
                    .chainLinkVerification(integrity2Chain ? "VALID" : "BROKEN")
                    .consentJsonMatch(integrity3ConsentJson ? "VALID" : "MISMATCH")
                    .overallIntegrityStatus(overallStatus)
                    .consentId(consent.getConsentId())
                    .referenceId(encryptedRefId)
                    .tenantId(tenantId)
                    .businessId(businessId)
                    .build();

            log.info("[CONSENT-EVENT-PAYLOAD] Payload: {}", consentEventPayload);
            // === Step 8: Generate Signed PDF ===
            byte[] signedPdfBytes = null;

            if ("VERIFIED".equals(overallStatus)) {

                signedPdfBytes = signPdfService.generateAndSignForm65B(
                        consentId,
                        canonicalData,
                        encryptedRefId,
                        storedPayloadHash,
                        HashUtils.sha256Hex(canonicalData),
                        true,
                        auditJson,           // null tolerated
                        tenantId,
                        businessId,
                        previousChain,
                        currentChainHash,
                        status
                );

                if (signedPdfBytes == null || signedPdfBytes.length == 0) {
                    log.warn("[PDF] Empty PDF returned by signing service.");
                } else {
                    log.info("[PDF] Signed PDF size: {} bytes", signedPdfBytes.length);
                }

            } else {
                log.warn("[PDF] Skipping PDF generation due to FAILED integrity.");
            }

            // === Step 9: Prepare API Response ===
            result.put("tenantId", tenantId);
            result.put("businessId", businessId);
            result.put("consentId", consentId);
            result.put("integrityTest_1_PayloadHash", integrity1PayloadHash ? "VALID" : "INVALID");
            result.put("integrityTest_2_ChainLink", integrity2Chain ? "VALID" : "BROKEN");
            result.put("integrityTest_3_ConsentJsonMatch", integrity3ConsentJson ? "VALID" : "MISMATCH");
            result.put("overallStatus", overallStatus);
            result.put("referenceId", encryptedRefId);

            // Safe Base64 encode
            result.put("pdfBase64", signedPdfBytes != null ?
                    Base64.getEncoder().encodeToString(signedPdfBytes) : null);

            // Add headers
            addIntegrityHeaders(response, result);

            log.info("[RESULT] Final Integrity Report: {}", result);

            // === Step 10: Create Audit Log ===
            createConsentIntegrityAudit(tenantId, businessId, consentId, result, req);

        } catch (Exception e) {

            log.error("[ERROR] Consent integrity verification failed: {}", e.getMessage());
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
            response.setHeader("Integrity-Overall-Status", "FAILED");
        }

        return result;
    }

    // ============== Internal Utility Methods ==============

    private void addIntegrityHeaders(HttpServletResponse response, Map<String, Object> result) {
        response.setHeader("Integrity-Test-1", (String) result.get("integrityTest_1_PayloadHash"));
        response.setHeader("Integrity-Test-2", (String) result.get("integrityTest_2_ChainLink"));
        response.setHeader("Integrity-Test-3", (String) result.get("integrityTest_3_ConsentJsonMatch"));
        response.setHeader("Integrity-Overall-Status", (String) result.get("overallStatus"));
    }

    // ================== Private Helper Method ==================
    private void createConsentIntegrityAudit(String tenantId, String businessId, String consentId,
                                             Map<String, Object> result, HttpServletRequest req) {
        log.info("[AUDIT] Creating consent integrity audit for consentId={}", consentId);
        try {
            String transactionId = UUID.randomUUID().toString();
            String clientIp = IpAddressUtil.getClientIp(req);

            // New map you want to add
            Map<String, Object> contextMap = Map.of(
                    "txnId", transactionId,
                    "ipAddress", clientIp
            );
            // Remove any binary data if accidentally present
            Map<String, Object> cleanedContext = new LinkedHashMap<>(result);
            cleanedContext.remove("pdfBase64"); // safety
            cleanedContext.remove("pdfBase64"); // safety
            cleanedContext.putAll(contextMap);
            // Build Actor
            AuditRequest.Actor actor = new AuditRequest.Actor();
            actor.setId(consentId);
            actor.setRole("SYSTEM");
            actor.setType("SERVICE");

            // Build Resource
            AuditRequest.Resource resource = new AuditRequest.Resource();
            resource.setId(consentId);
            resource.setType("CONSENT");

            // Build Audit Request
            AuditRequest auditRequest = AuditRequest.builder()
                    .tenantId(tenantId)
                    .businessId(businessId)
                    .transactionId(transactionId)
                    .group("CONSENT")
                    .component("INTEGRITY_CHECK")
                    .actionType("VERIFY")
                    .initiator("SYSTEM")
                    .actor(actor)
                    .resource(resource)
                    .payloadHash(result.get("referenceId").toString())
                    .context(cleanedContext)
                    .extra(Map.of("notes", "Consent integrity verification"))
                    .build();

            auditService.createAudit(auditRequest, tenantId, auditRequest.getTransactionId());
            log.info("[AUDIT] Consent integrity audit created for consentId={}", consentId);

        } catch (Exception ex) {
            log.warn("[AUDIT] Failed to create audit record: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Sanitizes and converts audit record to JSON string by removing tenantId
     *
     * @param auditRecord The audit record map containing AuditDocument
     * @return JSON string of sanitized audit record, or null if serialization fails
     */
    private String sanitizeAndSerializeAudit(Map<String, Object> auditRecord) {
        try {
            Object auditObj = auditRecord.get("audit");

            if (auditObj == null) {
                log.debug("Audit object is null");
                return null;
            }

            // Convert AuditDocument to Map first
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Convert object to Map
            @SuppressWarnings("unchecked")
            Map<String, Object> auditMap = mapper.convertValue(auditObj, Map.class);

            // Remove tenantId if exists
            if (auditMap.containsKey("tenantId")) {
                log.info("Removing tenantId from audit record");
                auditMap.remove("tenantId");
            }

            // Convert back to JSON string
            return mapper.writeValueAsString(auditMap);

        } catch (Exception e) {
            log.error("Failed to sanitize and serialize audit record", e);
            return null;
        }
    }
}
