package com.jio.auth.service.signing;

import com.jio.auth.constants.ErrorCode;
import com.jio.auth.constants.HeaderFields;
import com.jio.auth.enums.RequestorType;
import com.jio.auth.exception.CustomException;
import com.jio.auth.model.JwkKey;
import com.jio.auth.repository.BusinessKeyRepository;
import com.jio.auth.repository.JwkKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import com.auth0.jwt.JWTVerifier;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;


@Slf4j
@Service
public class RequestResponseSignatureService {


    @Autowired
    private BusinessKeyRepository businessKeyRepository;

    @Autowired
    private JwkKeyRepository jwkKeyRepository;

    @Value("${app.sign.response:false}")
    private boolean signResponse;

    @Value("${app.verify.request:false}")
    private boolean verifyRequest;

    public boolean verifyRequest(Map<String, String> payload, Map<String, String> headers) {
        if(!verifyRequest){
            log.info("Request verification is disabled");
            return true;
        }
        try {
            String tenantId = headers.get(HeaderFields.TENANT_ID_CODE);
            String businessId = headers.get(HeaderFields.BUSINESS_ID_CODE);
            String detachedJwt = headers.get(HeaderFields.SIGNATURE);
            String entity = headers.get(HeaderFields.REQUESTOR_TYPE);
            String encodedKey;
            if (tenantId == null || tenantId.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "Missing header: " + HeaderFields.TENANT_ID_CODE);
            }

            if (businessId == null || businessId.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "Missing header: " + HeaderFields.BUSINESS_ID_CODE);
            }

            RequestorType entityType = RequestorType.fromString(entity);

            switch (entityType) {
                case DATAPROCESSOR:
                    throw new CustomException(ErrorCode.INVALID_REQUEST, "The requestor type cannot call this API : " + entity);
                case DATAFIDUCIARY:
                    encodedKey = businessKeyRepository.findCertificateByBusinessId(tenantId, businessId);
                    break;
                default:
                    throw new CustomException(ErrorCode.INVALID_REQUEST, "The requestor type is unknow: " + entity);
            }

            String key = decodeCertificate(encodedKey);
            log.debug("Detached certificate:  "+key);
            String pem = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);

            // Reconstruct full JWT (header.payload.signature)
            String[] parts = detachedJwt.split("\\.\\.");
            if (parts.length != 2) {
                log.error("Invalid detached JWT format");
                throw new CustomException(ErrorCode.INVALID_REQUEST, "Invalid detached JWT format");
            }

            String header = parts[0];
            String signature = parts[1];

            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);
            String payloadBase64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String reconstructedJwt = header + "." + payloadBase64 + "." + signature;
            log.debug("Reconstructed JWT:  "+reconstructedJwt);

            Algorithm verifyAlgo = Algorithm.RSA256((RSAPublicKey) publicKey, null);
            JWTVerifier verifier = JWT.require(verifyAlgo).build();
            verifier.verify(reconstructedJwt);

            log.debug("Detached JWT verification successful");
            return true;

        }  catch (CustomException ce) {
            log.error(ce.getMessage());
            throw ce;
        } catch (Exception e) {
            log.error("Error verifying detached JWT: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Error verifying detached JWT: "+ e.getMessage());
        }
    }

    public String signResponse(Map<String, Object> payload) {
        if(!signResponse){
            log.info("Response signing is disabled");
            return null;
        }
        try {
            // Fetch JWK key (private)
            JwkKey jwk = jwkKeyRepository.findFirstByOrderByKidAsc();
            if (jwk == null || jwk.getD() == null) {
                log.error("No private JWK key found for signing");
                throw new CustomException(ErrorCode.INTERNAL_ERROR, "Unexpected error occured while signing the response. Unable to fetch the JCMS private key");
            }

            // Convert Base64URL → BigInteger
            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getN()));
            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getE()));
            BigInteger d = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getD()));
            BigInteger p = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getP()));
            BigInteger q = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getQ()));
            BigInteger dp = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getDp()));
            BigInteger dq = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getDq()));
            BigInteger qi = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getQi()));

            // Construct private key
            RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(n, e, d, p, q, dp, dq, qi);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            ObjectMapper mapper = new ObjectMapper();
            //Map<String, Object> orderedPayload = new TreeMap<>(payload);
            String payloadJson = mapper.writeValueAsString(payload);

            // Sign JWT
            Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
            String fullJwt = JWT.create()
                    .withPayload(payloadJson)
                    .sign(algorithm);
            log.info("full JWT: {}", fullJwt);

            // Convert to detached JWT (remove payload part)
            String[] parts = fullJwt.split("\\.");
            if (parts.length != 3) {
                log.error("Unexpected JWT format while signing");
                throw new CustomException(ErrorCode.INTERNAL_ERROR, "Unexpected JWT format while signing");
            }

            String detachedJwt = parts[0] + ".." + parts[2];
            log.info("Detached JWT generated successfully");
            return detachedJwt;

        } catch (Exception e) {
            log.error("Error while creating detached JWT: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "unable to sign the response: " + e.getMessage());
        }
    }

    public String signRequest(Map<String, Object> payload) {
        try {
            // Fetch JWK key (private)
            JwkKey jwk = jwkKeyRepository.findFirstByOrderByKidAsc();
            if (jwk == null || jwk.getD() == null) {
                log.error("No private JWK key found for signing");
                throw new CustomException(ErrorCode.INTERNAL_ERROR, "Unexpected error occured while signing the response. Unable to fetch the JCMS private key");
            }

            // Convert Base64URL → BigInteger
            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getN()));
            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getE()));
            BigInteger d = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getD()));
            BigInteger p = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getP()));
            BigInteger q = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getQ()));
            BigInteger dp = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getDp()));
            BigInteger dq = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getDq()));
            BigInteger qi = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.getQi()));

            // Construct private key
            RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(n, e, d, p, q, dp, dq, qi);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            ObjectMapper mapper = new ObjectMapper();
            //Map<String, Object> orderedPayload = new TreeMap<>(payload);
            String payloadJson = mapper.writeValueAsString(payload);

            // Sign JWT
            Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
            String fullJwt = JWT.create()
                    .withPayload(payloadJson)
                    .sign(algorithm);
            log.info("full JWT: {}", fullJwt);

            // Convert to detached JWT (remove payload part)
            String[] parts = fullJwt.split("\\.");
            if (parts.length != 3) {
                log.error("Unexpected JWT format while signing");
                throw new CustomException(ErrorCode.INTERNAL_ERROR, "Unexpected JWT format while signing");
            }

            String detachedJwt = parts[0] + ".." + parts[2];
            log.info("Detached JWT generated successfully");
            return detachedJwt;

        } catch (Exception e) {
            log.error("Error while creating detached JWT: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "unable to sign the response: " + e.getMessage());
        }
    }

    public boolean verifyResponse(Map<String, String> payload, Map<String, String> headers) {
        try {
            String tenantId = headers.get(HeaderFields.TENANT_ID_CODE);
            String businessId = headers.get(HeaderFields.BUSINESS_ID_CODE);
            String detachedJwt = headers.get(HeaderFields.SIGNATURE);
            String entity = headers.get(HeaderFields.REQUESTOR_TYPE);
            String encodedKey;
            if (tenantId == null || tenantId.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "Missing header: " + HeaderFields.TENANT_ID_CODE);
            }

            if (businessId == null || businessId.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "Missing header: " + HeaderFields.BUSINESS_ID_CODE);
            }

            RequestorType entityType = RequestorType.fromString(entity);

            switch (entityType) {
                case DATAPROCESSOR:
                    throw new CustomException(ErrorCode.INVALID_REQUEST, "The requestor type cannot call this API : " + entity);
                case DATAFIDUCIARY:
                    encodedKey = businessKeyRepository.findCertificateByBusinessId(tenantId, businessId);
                    break;
                default:
                    throw new CustomException(ErrorCode.INVALID_REQUEST, "The requestor type is unknow: " + entity);
            }

            String key = decodeCertificate(encodedKey);
            log.debug("Detached certificate:  "+key);
            String pem = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);

            // Reconstruct full JWT (header.payload.signature)
            String[] parts = detachedJwt.split("\\.\\.");
            if (parts.length != 2) {
                log.error("Invalid detached JWT format");
                throw new CustomException(ErrorCode.INVALID_REQUEST, "Invalid detached JWT format");
            }

            String header = parts[0];
            String signature = parts[1];

            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);
            String payloadBase64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String reconstructedJwt = header + "." + payloadBase64 + "." + signature;
            log.debug("Reconstructed JWT:  "+reconstructedJwt);

            Algorithm verifyAlgo = Algorithm.RSA256((RSAPublicKey) publicKey, null);
            JWTVerifier verifier = JWT.require(verifyAlgo).build();
            verifier.verify(reconstructedJwt);

            log.debug("Detached JWT verification successful");
            return true;

        }  catch (CustomException ce) {
            log.error(ce.getMessage());
            throw ce;
        } catch (Exception e) {
            log.error("Error verifying detached JWT: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Error verifying detached JWT: "+ e.getMessage());
        }
    }

    public String decodeCertificate(String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            return null;
        }

        try {
            // Case 1: Base64-encoded (has prefix)
            if (encodedValue.contains("base64,")) {
                String base64Part = encodedValue.substring(encodedValue.indexOf("base64,") + 7);
                byte[] decodedBytes = Base64.getDecoder().decode(base64Part);
                return new String(decodedBytes, StandardCharsets.UTF_8);
            }

            // Case 2: Plain PEM text (already decoded)
            if (encodedValue.contains("-----BEGIN PUBLIC KEY-----")) {
                log.debug("Certificate appears to be plain PEM format");
                return encodedValue;
            }

            // Case 3: Raw Base64 content without prefix (no BEGIN/END)
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(encodedValue);
                String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
                if (decoded.contains("-----BEGIN PUBLIC KEY-----")) {
                    return decoded;
                }
            } catch (IllegalArgumentException ignored) {
                // not valid Base64, ignore
            }

            log.warn("Unrecognized certificate format");
            return null;

        } catch (Exception e) {
            log.error("Failed to decode certificate: {}", e.getMessage(), e);
            return null;
        }
    }





}
