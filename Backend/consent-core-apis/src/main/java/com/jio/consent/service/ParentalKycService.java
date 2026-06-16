package com.jio.consent.service;

import com.jio.consent.constant.Constants;
import com.jio.consent.dto.Request.ParentalKycRequest;
import com.jio.consent.entity.ParentalKyc;
import com.jio.consent.repository.ParentalKycRepository;
import com.jio.consent.utils.RestApiManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ParentalKycService {

    private final RestApiManager restApiManager;
    private final ParentalKycRepository parentalKycRepository;
    private final com.jio.consent.repository.DigilockerConfigRepository digilockerConfigRepository;

    @Value("${digilocker.base.url:https://api.digitallocker.gov.in}")
    private String digilockerBaseUrl;

    @Value("${digilocker.proxy.enabled:false}")
    private boolean proxyEnabled;

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String codeVerifier;

    @Autowired
    public ParentalKycService(RestApiManager restApiManager, ParentalKycRepository parentalKycRepository, com.jio.consent.repository.DigilockerConfigRepository digilockerConfigRepository) {
        this.restApiManager = restApiManager;
        this.parentalKycRepository = parentalKycRepository;
        this.digilockerConfigRepository = digilockerConfigRepository;
    }

    public Map<String, String> createParentalKyc(ParentalKycRequest request, Map<String, String> headers) throws Exception {
        log.info("Creating parental kyc for tenant={} business={}", headers.get(Constants.TENANT_ID_HEADER), headers.get(Constants.BUSINESS_ID_HEADER));

        // Load Digilocker credentials from DB (by businessId, status="ACTIVE").
        String businessId = headers.get(Constants.BUSINESS_ID_HEADER);
        try {
            var cfgOpt = this.digilockerConfigRepository.findFirstByBusinessIdAndStatus(businessId, "ACTIVE");
            if (cfgOpt.isPresent()) {
                var cfg = cfgOpt.get();
                clientId = cfg.getClientId();
                clientSecret = cfg.getClientSecret();
                redirectUri = cfg.getRedirectUri();
                codeVerifier = cfg.getCodeVerifier();
                log.info("Loaded DigiLocker config from DB for business={}", businessId);
            } else {
                log.info("No active DigiLocker config found for business={}, falling back to properties", businessId);
            }
        } catch (Exception e) {
            log.warn("Error loading DigiLocker config from DB, using properties", e);
        }

        // Step 1: exchange code for access token
        Map<String, String> tokenReq = new HashMap<>();
        tokenReq.put("code", request.getCode());
        tokenReq.put("grant_type", "authorization_code");
        tokenReq.put("client_id", clientId);
        tokenReq.put("client_secret", clientSecret);
        tokenReq.put("redirect_uri", redirectUri);
        tokenReq.put("code_verifier", codeVerifier);

        log.info("Exchanging code for access token with DigiLocker for code={}", request.getCode());

        ResponseEntity<Map> tokenResp;
        if (proxyEnabled) {
            log.info("Using proxy for DigiLocker token request");
            tokenResp = this.restApiManager.postWithProxy(digilockerBaseUrl, "/public/oauth2/1/token", null, tokenReq, Map.class);
        } else {
            log.info("Direct call to DigiLocker token endpoint (no proxy)");
            tokenResp = this.restApiManager.post(digilockerBaseUrl, "/public/oauth2/1/token", null, tokenReq, Map.class);
        }
        
        if (tokenResp == null || tokenResp.getBody() == null) {
            throw new RuntimeException("Failed to get token from DigiLocker");
        }

        log.info("Received access token response from DigiLocker");

        Map body = tokenResp.getBody();
        String accessToken = (String) body.get("access_token");
        String digilockerId = body.get("digilockerid") != null ? body.get("digilockerid").toString() : null;
        String name = body.get("name") != null ? body.get("name").toString() : null;
        String dob = body.get("dob") != null ? body.get("dob").toString() : null;
        String referenceKey = body.get("reference_key") != null ? body.get("reference_key").toString() : null;
        String mobile = body.get("mobile") != null ? body.get("mobile").toString() : null;

        log.info("DigiLocker user: digilockerId={} name={} dob={} mobile={}", digilockerId, name, dob, mobile);
        log.info("Access Token: {}, Reference Key: {}", accessToken, referenceKey);
        
        // Step 2: get aadhaar xml. The endpoint may return application/xml so request as String and extract the element if present.
        Map<String, String> authHeader = Map.of("Authorization", "Bearer " + accessToken);
        
        ResponseEntity<String> aadhaarResp;
        if (proxyEnabled) {
            log.info("Using proxy for DigiLocker Aadhaar XML request");
            aadhaarResp = this.restApiManager.getWithProxy(digilockerBaseUrl, "/public/oauth2/3/xml/eaadhaar", authHeader, String.class);
        } else {
            log.info("Direct call to DigiLocker Aadhaar endpoint (no proxy)");
            aadhaarResp = this.restApiManager.get(digilockerBaseUrl, "/public/oauth2/3/xml/eaadhaar", authHeader, String.class);
        }
        
        if (aadhaarResp == null || aadhaarResp.getBody() == null) {
            throw new RuntimeException("Failed to get aadhaar from DigiLocker");
        }

        String aadhaarBody = aadhaarResp.getBody();
        String aadhaarEncrypted = null;
        try {
            // 1) Extract <aadhaar_xml> content if present
            int startTag = aadhaarBody.indexOf("<aadhaar_xml>");
            int endTag = aadhaarBody.indexOf("</aadhaar_xml>");
            if (startTag >= 0 && endTag > startTag) {
                int start = startTag + "<aadhaar_xml>".length();
                aadhaarEncrypted = aadhaarBody.substring(start, endTag).trim();
            }

            // 2) Fallback to full body
            if (aadhaarEncrypted == null || aadhaarEncrypted.isEmpty()) {
                aadhaarEncrypted = aadhaarBody.trim();
            }

            // 3) Ensure Base64 encoding (use UTF-8 for canonical encoding)
            if (!isBase64AndCanonical(aadhaarEncrypted)) {
                aadhaarEncrypted = Base64.getEncoder().encodeToString(aadhaarEncrypted.getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            log.warn("Failed to parse Aadhaar XML response, saving encoded raw body", e);
            try {
                aadhaarEncrypted = Base64.getEncoder().encodeToString(aadhaarBody.trim().getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                aadhaarEncrypted = aadhaarBody.trim();
            }
        }

            // Persist to MongoDB
            ParentalKyc entity = ParentalKyc.builder()
                .tenantId(headers.get(Constants.TENANT_ID_HEADER))
                .businessId(headers.get(Constants.BUSINESS_ID_HEADER))
                .name(name)
                .mobileNo(mobile)
                .dob(dob)
                .type(com.jio.consent.dto.ParentalKycType.DIGILOCKER)
                .status(com.jio.consent.dto.ParentalKycStatus.VERIFIED)
                .kycReferenceId(referenceKey)
                .parentalReferenceId(digilockerId)
                .aadhaarEncryptedData(aadhaarEncrypted)
                .build();

        ParentalKyc saved = this.parentalKycRepository.save(entity);
        log.info("Saved parental kyc id={} ref={}", saved.getId(), saved.getKycReferenceId());

        Map<String, String> resp = new HashMap<>();
        resp.put("parental_kyc", saved.getKycReferenceId());
        resp.put("parental_reference_id", saved.getParentalReferenceId());
        return resp;
    }

    private boolean isBase64AndCanonical(String s) {
        if (s == null || s.trim().isEmpty()) return false;
        String trimmed = s.trim();
        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            String reencoded = Base64.getEncoder().encodeToString(decoded);
            return reencoded.equals(trimmed);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}