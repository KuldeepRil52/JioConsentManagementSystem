package com.jio.digigov.auditmodule.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.auditmodule.dto.Consent;
import com.jio.digigov.auditmodule.dto.ConsentDto;
import com.jio.digigov.auditmodule.dto.CookieConsentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Map;
import java.util.Optional;

/**
 * Utility class for performing all integrity and chain-related verifications.
 */
@Slf4j
public class IntegrityCheckUtil {

    private static final String GENESIS_CHAIN =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**  Integrity Test #1: Validate Payload Hash */
    public static boolean validatePayloadHash(String canonicalJson, String storedHash) {
        String computedHash = HashUtils.sha256Hex(canonicalJson);
        boolean match = computedHash.equals(storedHash);
        log.info("[INTEGRITY-1] Payload Hash Verification → {}", match ? "VALID " : "INVALID");
        return match;
    }

    /**  Integrity Test #2: Validate Chain Continuity */
    public static boolean validateChain(String previousChain, String storedHash, String currentChainHash) {
        if (previousChain == null || previousChain.isEmpty() || previousChain.equals(GENESIS_CHAIN)) {
            log.info("[INTEGRITY-2] Chain Validation → GENESIS CHAIN, considered VALID");
            return true;
        }
        String computedChain = HashUtils.sha256Hex(previousChain + storedHash);
        boolean valid = computedChain.equals(currentChainHash);
        log.info("[INTEGRITY-2] Chain Validation → {}", valid ? "CHAIN VALID " : "CHAIN BROKEN");
        return valid;
    }

    /**  Integrity Test #3: Compare consentJsonString with canonical data */
    public static boolean validateConsentJsonMatch(String canonicalJson, String consentJson) {
        boolean valid = canonicalJson.equals(consentJson);
        log.info("[INTEGRITY-3] Consent JSON Match → {}", valid ? "MATCH" : "MISMATCH ");
        return valid;
    }

    /**
     * 🔹 Fetch the previous consent’s currentChainHash for the given current consent.
     * Returns GENESIS_CHAIN if not found.
     */
    public static String fetchPreviousConsentChain(MongoTemplate mongoTemplate, Consent currentConsent) {
        try {
            // Build query to find previous consent
            Query query = new Query();

            query.addCriteria(
                    Criteria.where("consentId").is(currentConsent.getConsentId())
                            .and("createdAt").gt(currentConsent.getCreatedAt())
            );

            // Sort by createdAt descending → latest previous
            query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
            query.limit(1);

            ConsentDto previousConsent = mongoTemplate.findOne(query, ConsentDto.class, "consents");

            if (previousConsent == null) {
                log.warn("[CHAIN-FETCH] No previous consent found before {}. Returning GENESIS_CHAIN.",
                        currentConsent.getConsentId());
                return GENESIS_CHAIN;
            }

            // Return previous currentChainHash
            return Optional.ofNullable(previousConsent.getCurrentChainHash()).orElse(GENESIS_CHAIN);

        } catch (Exception e) {
            log.error("[CHAIN-FETCH] Error fetching previous consent: {}", e.getMessage(), e);
            return GENESIS_CHAIN;
        }
    }


    /**
     * 🔹 Fetch the previous consent’s currentChainHash for the given current consent.
     * Returns GENESIS_CHAIN if not found.
     */
    public static String fetchPreviousConsentChain(MongoTemplate mongoTemplate, CookieConsentDto currentConsent) {
        try {
            // Build query to find previous consent
            Query query = new Query();

            query.addCriteria(
                    Criteria.where("consentId").is(currentConsent.getConsentId())
                            .and("createdAt").gt(currentConsent.getCreatedAt())
            );

            // Sort by createdAt descending → latest previous
            query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
            query.limit(1);

            CookieConsentDto previousConsent = mongoTemplate.findOne(query, CookieConsentDto.class, "cookie_consents");

            if (previousConsent == null) {
                log.warn("[CHAIN-FETCH] No previous consent found before {}. Returning GENESIS_CHAIN.",
                        currentConsent.getConsentId());
                return GENESIS_CHAIN;
            }

            // Return previous currentChainHash
            return Optional.ofNullable(previousConsent.getCurrentChainHash()).orElse(GENESIS_CHAIN);

        } catch (Exception e) {
            log.error("[CHAIN-FETCH] Error fetching previous consent: {}", e.getMessage(), e);
            return GENESIS_CHAIN;
        }
    }
}