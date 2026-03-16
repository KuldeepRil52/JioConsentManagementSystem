package com.jio.digigov.auditmodule.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jio.digigov.auditmodule.client.vault.VaultManager;
import com.jio.digigov.auditmodule.client.vault.response.EncryptPayloadResponse;
import com.jio.digigov.auditmodule.config.LocalDateTypeAdapter;
import com.jio.digigov.auditmodule.entity.AuditDocument;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility for blockchain-style chaining of audit entries.
 */
@Slf4j
@Component
public class AuditChainUtil {

    private final VaultManager vaultManager;

    @Autowired
    public AuditChainUtil(VaultManager vaultManager) {
        this.vaultManager = vaultManager;
    }

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private static final String GENESIS_CHAIN =
            "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Fetches the most recent chain hash (the "currentChain" of the latest document)
     * from the audit collection for continuity.
     */
    public static <T> String fetchLatestChain(MongoTemplate mongoTemplate, Class<T> entityClass) {
        try {
            Query query = new Query().with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(1);

            // Fetch the latest document as a raw BSON document
            Document latestDoc = mongoTemplate.findOne(query, Document.class, mongoTemplate.getCollectionName(entityClass));

            if (latestDoc == null) {
                return GENESIS_CHAIN;
            }

            Object chain = latestDoc.get("currentChainHash");
            return (chain != null) ? chain.toString() : GENESIS_CHAIN;

        } catch (Exception e) {
            log.error("Failed to fetch latest chain hash", e);
            return GENESIS_CHAIN;
        }
    }

    /**
     * Canonicalizes the AuditRequest, encodes it, computes hashes,
     * and populates blockchain fields.
     */
    public void populateChainFields(AuditDocument req, String previousChainHash,
                                    ObjectMapper objectMapper, String tenantId) throws Exception {

        if (previousChainHash == null || previousChainHash.isEmpty()) {
            previousChainHash = GENESIS_CHAIN;
        }
        log.info("Populating chain fields for audit id: {} with previousChainHash: {}",
                req.getAuditId(), previousChainHash);

        // Convert request to map for canonicalization
        Map<String, Object> auditMap = objectMapper.convertValue(req, Map.class);
        auditMap.remove("id");
        auditMap.remove("payloadHash");
        auditMap.remove("previousChainHash");
        auditMap.remove("currentChainHash");
        auditMap.remove("encryptedReferenceId");

        // Step 1: Canonicalize JSON (sort all keys)
        String auditJsonString = canonicalizeJson(auditMap);

        log.info("Canonicalized audit JSON for audit id {}: {}", req.getAuditId(), auditJsonString);
        String encryptedReferenceId = null;
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTypeAdapter())
                    .create();
            String canonicalData = gson.toJson(auditMap).replaceAll("\\s+", "");

            EncryptPayloadResponse encryptResponse = this.vaultManager.encryptPayload(
                    tenantId,
                    req.getBusinessId(),
                    "auditId",
                    req.getAuditId(),
                    canonicalData
            );
            log.info("Successfully encrypted audit payload for audit id: {} encryptResponse : {}",
                    req.getAuditId(), encryptResponse);

            // Store encryptedReferenceId in the audit entity
            encryptedReferenceId = encryptResponse.getReferenceId();
            req.setEncryptedReferenceId(encryptedReferenceId);
        } catch (Exception e) {
            log.error("Failed to encrypt audit payload for audit id: {}, error: {}",
                    req.getAuditId(), e.getMessage(), e);
            // Continue even if encryption fails
        }

        // Step 2: Hash of canonical data
        String payloadHash = sha256Hex(auditJsonString);

        // Step 3: Chain hash = sha256(previousChain + auditHash)
        String currentChainHash = sha256Hex(previousChainHash + payloadHash);

        // Step 4: Populate fields into request
        req.setPayloadHash(payloadHash);
        req.setCurrentChainHash(currentChainHash);
        req.setEncryptedReferenceId(encryptedReferenceId);
    }

    /**
     * Canonicalizes JSON to deterministic order.
     */
    private static String canonicalizeJson(Map<String, Object> map) throws Exception {
        Map<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getValue() instanceof Map)
                sorted.put(e.getKey(), new TreeMap<>((Map<String, Object>) e.getValue()));
            else
                sorted.put(e.getKey(), e.getValue());
        }
        return mapper.writeValueAsString(sorted);
    }

    /**
     * Compute SHA-256 hex digest.
     */
    private static String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    // Helper for consumer to recompute chain safely
    public static String recomputeChain(String prevHash, String payloadHash) throws Exception {
        if (prevHash == null || prevHash.isEmpty()) prevHash = GENESIS_CHAIN;
        return sha256Hex(prevHash + payloadHash);
    }
}