package com.jio.vault.util;

import com.jio.vault.constants.ErrorCode;
import com.jio.vault.documents.ClientPublicCert;
import com.jio.vault.exception.CustomException;
import com.jio.vault.repository.ClientPublicCertRepositoryCustom;
import com.jio.vault.dto.VerifyJwtSignResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class JwtVerifierUtil {

    //private final ClientPublicCertRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClientPublicCertRepositoryCustom customRepo;

    public JwtVerifierUtil(ClientPublicCertRepositoryCustom customRepo) {
        this.customRepo = customRepo;
    }

    public VerifyJwtSignResponse verifyJwt(String tenantId, String businessId, String jwt) {
        try {
            String dbName = "tenant_db_" + tenantId;
//            ClientPublicCert cert = customRepo.findByBusinessIdAndTenantIdDynamic(dbName, businessId, tenantId)
//                    .orElseThrow(() -> new CustomException(
//                            ErrorCode.INVALID_REQUEST,
//                            "No certificate exists for tenantId=" + tenantId + " and businessId=" + businessId
//                    ));
            ClientPublicCert cert =
                    customRepo.findByBusinessIdAndTenantIdDynamic(dbName, businessId, tenantId)
                            .orElseThrow(() -> {

                                log.error("No certificate found for tenantId={} and businessId={}", tenantId, businessId);

                                return new CustomException(
                                        ErrorCode.INVALID_REQUEST,
                                        "No certificate exists for tenantId=" + tenantId + " and businessId=" + businessId
                                );
                            });

            String publicKeyPem = cert.getPublicKeyPem();

            String publicKeyContent = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decodedKey = Base64.getDecoder().decode(publicKeyContent);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedKey));

            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                return new VerifyJwtSignResponse(false, null);
            }

            String header = parts[0].replaceAll("\\s+", "");
            String payload = parts[1].replaceAll("\\s+", "");
            String signatureB64Url = parts[2].replaceAll("\\s+", "");

            byte[] signatureBytes = Base64.getUrlDecoder().decode(signatureB64Url);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update((header + "." + payload).getBytes());
            boolean valid = signature.verify(signatureBytes);
            if (!valid) {
                log.error("Signature verification failed for tenantId={} and businessId={}", tenantId, businessId);
                return new VerifyJwtSignResponse(false, null);
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(payload);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);

            if (claims.containsKey("exp")) {
                Object expObj = claims.get("exp");
                if (expObj instanceof String) {
                    try {
                        OffsetDateTime expTime = OffsetDateTime.parse((String) expObj);
                        Instant now = Instant.now();
                        if (now.isAfter(expTime.toInstant())) {
                            log.error("JWT has expired. Expiry {}", expTime);
                            return new VerifyJwtSignResponse(false, claims);
                        }
                    } catch (DateTimeParseException e) {
                        return new VerifyJwtSignResponse(false, claims);
                    }
                } else if (expObj instanceof Number) {
                    long now = System.currentTimeMillis() / 1000L;
                    long exp = ((Number) expObj).longValue();
                    if (now > exp) {
                        log.error("JWT has expired. Expiry in long "+ exp);
                        return new VerifyJwtSignResponse(false, claims);
                    }
                }
            }else{
                log.info("JWT seems to not have the exp");
            }
            return new VerifyJwtSignResponse(true, claims);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Failed verify the JWT {}", e.getMessage());
            return new VerifyJwtSignResponse(false, null);
        }
    }
}

