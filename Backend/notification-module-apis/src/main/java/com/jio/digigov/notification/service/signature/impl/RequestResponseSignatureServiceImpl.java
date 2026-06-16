package com.jio.digigov.notification.service.signature.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.jio.digigov.notification.entity.signature.JwkKey;
import com.jio.digigov.notification.enums.RecipientType;
import com.jio.digigov.notification.exception.SignatureGenerationException;
import com.jio.digigov.notification.exception.SignatureVerificationException;
import com.jio.digigov.notification.repository.signature.BusinessKeyRepository;
import com.jio.digigov.notification.repository.signature.JwkKeyRepository;
import com.jio.digigov.notification.service.signature.RequestResponseSignatureService;
import com.jio.digigov.notification.util.SignatureJsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Implementation of RequestResponseSignatureService for JWS signature operations.
 *
 * <p>This service provides RSA256 signature verification for incoming requests
 * and signature generation for outgoing responses using detached JWT format.</p>
 *
 * @since 2.0.0
 */
@Slf4j
@Service
public class RequestResponseSignatureServiceImpl implements RequestResponseSignatureService {

    // Header constants (lowercase with hyphen format)
    private static final String HEADER_TENANT_ID = "tenant-id";
    private static final String HEADER_BUSINESS_ID = "business-id";
    private static final String HEADER_SIGNATURE = "x-jws-signature";
    private static final String HEADER_REQUESTOR_TYPE = "requestor-type";
    private static final String HEADER_DATA_PROCESSOR_ID = "data-processor-id";

    @Autowired
    private BusinessKeyRepository businessKeyRepository;

    @Autowired
    private JwkKeyRepository jwkKeyRepository;

    @Autowired
    private SignatureJsonSerializer jsonSerializer;

    @Value("${app.sign.response:false}")
    private boolean signResponse;

    @Value("${app.verify.request:false}")
    private boolean verifyRequest;

    @Override
    public boolean isSignResponseEnabled() {
        return signResponse;
    }

    @Override
    public boolean isVerifyRequestEnabled() {
        return verifyRequest;
    }

    @Override
    public boolean verifyRequest(Object payload, Map<String, String> headers)
            throws SignatureVerificationException {

        if (!verifyRequest) {
            log.info("Request verification is disabled (app.verify.request=false) - skipping verification");
            return true;
        }

        try {
            // Extract required headers
            String tenantId = headers.get(HEADER_TENANT_ID);
            String businessId = headers.get(HEADER_BUSINESS_ID);
            String dataProcessorId = headers.get(HEADER_DATA_PROCESSOR_ID);
            String detachedJwt = headers.get(HEADER_SIGNATURE);
            String requestorTypeStr = headers.get(HEADER_REQUESTOR_TYPE);

            // Validate required headers
            validateVerificationHeaders(tenantId, requestorTypeStr, detachedJwt);

            // Parse requestor type
            RecipientType requestorType = parseRecipientType(requestorTypeStr);

            // Fetch certificate based on requestor type
            String encodedCertificate = fetchCertificate(
                    tenantId, businessId, dataProcessorId, requestorType);

            log.info("Fetched certificate");
            log.debug(encodedCertificate);

            // Decode and extract public key from certificate
            PublicKey publicKey = extractPublicKeyFromCertificate(encodedCertificate);

            // Serialize payload to JSON
            String payloadJson = jsonSerializer.serializeToSortedJson(payload);

            // Reconstruct full JWT from detached format
            String reconstructedJwt = reconstructJwt(detachedJwt, payloadJson);

            // Verify signature
            verifyJwtSignature(reconstructedJwt, publicKey);

            log.info("Request signature verification successful for tenant={}, requestorType={}",
                    tenantId, requestorType);
            return true;

        } catch (SignatureVerificationException e) {
            // Re-throw verification exceptions
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during signature verification: {}", e.getMessage());
            throw new SignatureVerificationException(
                    "Signature verification failed due to unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public String signResponse(String tenantId, Object payload) throws SignatureGenerationException {

        if (!signResponse) {
            log.info("Response signing is disabled (app.sign.response=false) - skipping signing");
            return null;
        }

        try {
            // Fetch JWK private key for the tenant
            JwkKey jwk = jwkKeyRepository.findFirstByKtyAndUse(tenantId, "RSA", "sig");

            if (jwk == null) {
                // Fallback: try finding by use only
                jwk = jwkKeyRepository.findFirstByUse(tenantId, "sig");
            }

            if (jwk == null) {
                // Fallback: try finding by kid ordering (for keys with kid field)
                jwk = jwkKeyRepository.findFirstByOrderByKidAsc(tenantId);
            }

            if (jwk == null) {
                log.error("No JWK key found in auth_key collection for tenant={}", tenantId);
                throw SignatureGenerationException.jwkKeyNotFound();
            }

            log.debug("Using JWK key: kty={}, use={}, kid={} for tenant={}",
                    jwk.getKty(), jwk.getUse(), jwk.getKid(), tenantId);

            if (jwk.getD() == null || jwk.getD().isBlank()) {
                log.error("JWK key missing private exponent (d parameter)");
                throw SignatureGenerationException.missingRsaParameter("d (private exponent)");
            }

            // Convert JWK to RSA private key
            RSAPrivateKey privateKey = convertJwkToPrivateKey(jwk);

            // Serialize payload to JSON
            String payloadJson = jsonSerializer.serializeToSortedJson(payload);

            // Create full JWT
            Algorithm algorithm = Algorithm.RSA256(null, privateKey);
            String fullJwt = JWT.create()
                    .withPayload(payloadJson)
                    .sign(algorithm);

            // Convert to detached format (header..signature)
            String detachedJwt = convertToDetachedJwt(fullJwt);

            log.info("Response signature generated successfully (length: {})", detachedJwt.length());
            return detachedJwt;

        } catch (SignatureGenerationException e) {
            // Re-throw generation exceptions
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during signature generation: {}", e.getMessage());
            throw new SignatureGenerationException(
                    "Signature generation failed due to unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public String decodeCertificate(String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            return null;
        }

        try {
            if (encodedValue.contains("base64,")) {
                String base64Part = encodedValue.substring(encodedValue.indexOf("base64,") + 7);
                byte[] decodedBytes = Base64.getDecoder().decode(base64Part);
                return new String(decodedBytes, StandardCharsets.UTF_8);
            }

            if (encodedValue.contains("-----BEGIN PUBLIC KEY-----")) {
                log.debug("Certificate appears to be plain PEM format");
                return encodedValue;
            }


            try {
                byte[] decodedBytes = Base64.getDecoder().decode(encodedValue);
                String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
                if (decoded.contains("-----BEGIN PUBLIC KEY-----")) {
                    return decoded;
                }
            } catch (IllegalArgumentException ignored) {

            }

            log.warn("Unrecognized certificate format");
            return null;

        } catch (Exception e) {
            log.error("Failed to decode certificate: {}", e.getMessage());
            return null;
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * Validates required headers for signature verification.
     */
    private void validateVerificationHeaders(String tenantId, String requestorType, String signature)
            throws SignatureVerificationException {

        if (tenantId == null || tenantId.isBlank()) {
            throw SignatureVerificationException.missingRequiredHeaders("tenant-id");
        }

        if (requestorType == null || requestorType.isBlank()) {
            throw SignatureVerificationException.missingRequiredHeaders("requestor-type");
        }

        if (signature == null || signature.isBlank()) {
            throw SignatureVerificationException.missingSignature();
        }
    }

    /**
     * Parses requestor type string to RecipientType enum.
     */
    private RecipientType parseRecipientType(String requestorTypeStr)
            throws SignatureVerificationException {
        try {
            return RecipientType.valueOf(requestorTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid requestor type: {}", requestorTypeStr);
            throw new SignatureVerificationException(
                    "Invalid requestor-type: " + requestorTypeStr + ". Must be DATA_FIDUCIARY or DATA_PROCESSOR")
                    .addMetadata("requestorType", requestorTypeStr);
        }
    }

    /**
     * Fetches certificate based on requestor type.
     */
    private String fetchCertificate(String tenantId, String businessId,
                                     String dataProcessorId, RecipientType requestorType)
            throws SignatureVerificationException {

        String encodedCertificate;

        switch (requestorType) {
            case DATA_FIDUCIARY:
                if (businessId == null || businessId.isBlank()) {
                    throw SignatureVerificationException.missingRequiredHeaders(
                            "business-id (required for DATA_FIDUCIARY)");
                }

                encodedCertificate = businessKeyRepository.findCertificateByBusinessId(tenantId, businessId);
                if (encodedCertificate == null) {
                    throw SignatureVerificationException.certificateNotFound("DATA_FIDUCIARY", businessId);
                }
                break;

            case DATA_PROCESSOR:
                if (dataProcessorId == null || dataProcessorId.isBlank()) {
                    throw SignatureVerificationException.missingRequiredHeaders(
                            "data-processor-id (required for DATA_PROCESSOR)");
                }

                encodedCertificate = businessKeyRepository.findCertificateByDataProcessorId(
                        tenantId, dataProcessorId);
                if (encodedCertificate == null) {
                    throw SignatureVerificationException.certificateNotFound("DATA_PROCESSOR", dataProcessorId);
                }
                break;

            default:
                throw new SignatureVerificationException(
                        "Unsupported requestor type: " + requestorType)
                        .addMetadata("requestorType", requestorType.name());
        }

        return encodedCertificate;
    }

    /**
     * Extracts RSA public key from PEM certificate.
     */
    private PublicKey extractPublicKeyFromCertificate(String encodedCertificate)
            throws SignatureVerificationException {
        try {
            // Decode certificate
            String certificate = decodeCertificate(encodedCertificate);

            // Extract PEM content (remove headers and whitespace)
            String pem = certificate
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");


            byte[] keyBytes = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);

            return publicKey;
        } catch (Exception e) {
            log.error("Failed to extract public key from certificate: {}", e.getMessage());
            throw SignatureGenerationException.cryptoOperationFailed(
                    "Extract public key from certificate", e);
        }
    }

    /**
     * Reconstructs full JWT from detached format.
     */
    private String reconstructJwt(String detachedJwt, String payloadJson)
            throws SignatureVerificationException {
        try {
            // Split detached JWT (header..signature)
            String[] parts = detachedJwt.split("\\.\\.");
            if (parts.length != 2) {
                log.error("Invalid detached JWT format: expected 'header..signature', got {} parts", parts.length);
                throw SignatureVerificationException.invalidFormat();
            }

            String header = parts[0];
            String signature = parts[1];

            // Base64URL encode payload
            String payloadBase64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Reconstruct full JWT: header.payload.signature
            String reconstructedJwt = header + "." + payloadBase64 + "." + signature;

            log.debug("Reconstructed full JWT from detached format (length: {})", reconstructedJwt.length());
            return reconstructedJwt;

        } catch (SignatureVerificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reconstruct JWT: {}", e.getMessage());
            throw new SignatureVerificationException("Failed to reconstruct JWT: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies JWT signature using RSA public key.
     */
    private void verifyJwtSignature(String jwt, PublicKey publicKey)
            throws SignatureVerificationException {
        try {
            Algorithm verifyAlgo = Algorithm.RSA256((RSAPublicKey) publicKey, null);
            JWTVerifier verifier = JWT.require(verifyAlgo).build();
            verifier.verify(jwt);

            log.debug("JWT signature verification successful");

        } catch (Exception e) {
            log.error("JWT signature verification failed: {}", e.getMessage());
            throw SignatureVerificationException.signatureMismatch()
                    .addMetadata("error", e.getMessage());
        }
    }

    /**
     * Converts JWK to RSA private key.
     */
    private RSAPrivateKey convertJwkToPrivateKey(JwkKey jwk)
            throws SignatureGenerationException {
        try {
            // Validate required JWK parameters
            validateJwkParameters(jwk);

            // Convert Base64URL to BigInteger
            BigInteger n = decodeBase64UrlToBigInteger(jwk.getN(), "n (modulus)");
            BigInteger e = decodeBase64UrlToBigInteger(jwk.getE(), "e (exponent)");
            BigInteger d = decodeBase64UrlToBigInteger(jwk.getD(), "d (private exponent)");
            BigInteger p = decodeBase64UrlToBigInteger(jwk.getP(), "p (prime1)");
            BigInteger q = decodeBase64UrlToBigInteger(jwk.getQ(), "q (prime2)");
            BigInteger dp = decodeBase64UrlToBigInteger(jwk.getDp(), "dp (exponent1)");
            BigInteger dq = decodeBase64UrlToBigInteger(jwk.getDq(), "dq (exponent2)");
            BigInteger qi = decodeBase64UrlToBigInteger(jwk.getQi(), "qi (coefficient)");

            // Construct RSA private key
            RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(n, e, d, p, q, dp, dq, qi);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            log.debug("Successfully converted JWK to RSA private key");
            return (RSAPrivateKey) privateKey;

        } catch (SignatureGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert JWK to private key: {}", e.getMessage());
            throw SignatureGenerationException.cryptoOperationFailed("Convert JWK to RSA private key", e);
        }
    }

    /**
     * Validates JWK parameters.
     */
    private void validateJwkParameters(JwkKey jwk) throws SignatureGenerationException {
        if (jwk.getN() == null || jwk.getN().isBlank()) {
            throw SignatureGenerationException.missingRsaParameter("n (modulus)");
        }
        if (jwk.getE() == null || jwk.getE().isBlank()) {
            throw SignatureGenerationException.missingRsaParameter("e (exponent)");
        }
        if (jwk.getD() == null || jwk.getD().isBlank()) {
            throw SignatureGenerationException.missingRsaParameter("d (private exponent)");
        }
        if (jwk.getP() == null || jwk.getP().isBlank()) {
            throw SignatureGenerationException.missingRsaParameter("p (prime1)");
        }
        if (jwk.getQ() == null || jwk.getQ().isBlank()) {
            throw SignatureGenerationException.missingRsaParameter("q (prime2)");
        }
        if (jwk.getDp() == null || jwk.getDp().isBlank()) {
            throw SignatureGenerationException.missingRsaParameter("dp (exponent1)");
        }
        if (jwk.getDq() == null || jwk.getDq().isBlank()) {
            throw SignatureGenerationException.missingRsaParameter("dq (exponent2)");
        }
        if (jwk.getQi() == null || jwk.getQi().isBlank()) {
            throw SignatureGenerationException.missingRsaParameter("qi (coefficient)");
        }
    }

    /**
     * Decodes Base64URL string to BigInteger.
     */
    private BigInteger decodeBase64UrlToBigInteger(String base64Url, String parameterName)
            throws SignatureGenerationException {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(base64Url);
            return new BigInteger(1, bytes);
        } catch (Exception e) {
            log.error("Failed to decode Base64URL parameter {}: {}", parameterName, e.getMessage());
            throw SignatureGenerationException.base64DecodingFailed(parameterName, e);
        }
    }

    /**
     * Converts full JWT to detached format (header..signature).
     */
    private String convertToDetachedJwt(String fullJwt) throws SignatureGenerationException {
        String[] parts = fullJwt.split("\\.");
        if (parts.length != 3) {
            log.error("Unexpected JWT format: expected 3 parts, got {}", parts.length);
            throw new SignatureGenerationException("Unexpected JWT format while signing");
        }

        // Return detached format: header..signature
        String detachedJwt = parts[0] + ".." + parts[2];
        log.debug("Converted full JWT to detached format");
        return detachedJwt;
    }
}
