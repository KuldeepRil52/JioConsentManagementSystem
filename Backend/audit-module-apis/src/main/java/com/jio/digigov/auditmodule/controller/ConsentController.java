package com.jio.digigov.auditmodule.controller;

import com.jio.digigov.auditmodule.service.ConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/consent")
public class ConsentController {

    private final ConsentService consentService;

    /**
     * API: Consent Integrity Check
     */
    @GetMapping("/checkIntegrity")
    public ResponseEntity<Map<String, Object>> verifyConsentIntegrity(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-Business-ID") String businessId,
            @RequestHeader("consentId") String consentId,
            @RequestHeader(value = "X-Consent-Type", defaultValue = "consent") String consentType,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {

        log.info("Starting integrity verification for tenantId={}, businessId={}, consentId={}, consentType={}",
                tenantId, businessId, consentId, consentType);

        try {
            Map<String, Object> result;

            switch (consentType.toLowerCase()) {
                case "consent_cookies":
                    log.debug("Verifying integrity for consent_cookies");
                    result = consentService.verifyConsentCookiesIntegrity(tenantId, businessId, consentId, response,servletRequest);
                    break;

                case "consent":
                    log.debug("Verifying integrity for consent");
                    result = consentService.verifyConsentIntegrity(tenantId, businessId, consentId, response, servletRequest);
                    break;

                default:
                    log.warn("Invalid X-Consent-Type provided: '{}'. Allowed values are 'consent' or 'consent_cookies'", consentType);
                    Map<String, Object> error = new HashMap<>();
                    error.put("status", "ERROR");
                    error.put("message", "Invalid X-Consent-Type header. Allowed values: 'consent' or 'consent_cookies'");
                    return ResponseEntity.badRequest().body(error);
            }

            log.info("Integrity verification successful for consentId={}", consentId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Integrity check failed for consentId={}: {}", consentId, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
