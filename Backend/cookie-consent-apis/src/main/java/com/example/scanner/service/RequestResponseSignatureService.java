package com.example.scanner.service;


import com.auth0.jwt.algorithms.Algorithm;
import com.example.scanner.constants.ErrorCodes;
import com.example.scanner.dto.JwkKey;
import com.example.scanner.enums.RequestorType;
import com.example.scanner.exception.CustomException;
import com.example.scanner.repository.JwkKeyRepository;
import com.example.scanner.repository.impl.BusinessKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.auth0.jwt.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.TreeMap;


@Slf4j
@Service
public class RequestResponseSignatureService {

    private static final String HEADER_TENANT_ID = "tenant-id";
    private static final String HEADER_BUSINESS_ID = "business-id";
    private static final String HEADER_SIGNATURE = "x-jws-signature";
    private static final String HEADER_ENTITY = "requestor-type";
    private static final String HEADER_DATA_PROCESSOR_ID = "data-processor-id";

    @Autowired
    private BusinessKeyRepository businessKeyRepository;

    @Autowired
    private JwkKeyRepository jwkKeyRepository;

    @Value("${app.sign.response:false}")
    private boolean signResponse;

    @Value("${app.verify.request:false}")
    private boolean verifyRequest;

    public boolean verifyRequest(Map<String, Object> payload, Map<String, String> headers) {
        if(!verifyRequest){
            log.info("Request verification is disabled");
            return true;
        }
        try {
            String tenantId = headers.get(HEADER_TENANT_ID);
            String businessId = headers.get(HEADER_BUSINESS_ID);
            String dataprocessorId = headers.get(HEADER_DATA_PROCESSOR_ID);
            String detachedJwt = headers.get(HEADER_SIGNATURE);
            String entity = headers.get(HEADER_ENTITY);
            String encodedKey = "";
            RequestorType entityType = RequestorType.fromString(entity);

            switch (entityType) {
                case DATAPROCESSOR:
                    encodedKey = businessKeyRepository.findCertOfDataProcessor(tenantId, dataprocessorId);
                case DATAFIDUCIARY:
                    encodedKey = businessKeyRepository.findCertificateByBusinessId(tenantId, businessId);
                    break;
                default:
                    throw new CustomException(ErrorCodes.INVALID_REQUEST, "Unknown entity type: " + entity);
            }

            String key = decodeCertificate(encodedKey);
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
                throw new CustomException(ErrorCodes.INVALID_REQUEST, "Invalid detached JWT format");
            }

            String header = parts[0];
            String signature = parts[1];

            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);
            String payloadBase64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String reconstructedJwt = header + "." + payloadBase64 + "." + signature;

            Algorithm verifyAlgo = Algorithm.RSA256((RSAPublicKey) publicKey, null);
            JWTVerifier verifier = JWT.require(verifyAlgo).build();
            verifier.verify(reconstructedJwt);

            log.info("Detached JWT verification successful");
            return true;

        }  catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("Error verifying detached JWT:");
            throw new CustomException(ErrorCodes.INTERNAL_ERROR, "Error verifying detached JWT: "+ e.getMessage());
        }
    }

    public String signRequest(Map<String, Object> payload) {
        if(!signResponse){
            log.info("Response signing is disabled");
            return null;
        }
        try {
            // Fetch JWK key (private)
            JwkKey jwk = jwkKeyRepository.findFirstByOrderByKidAsc();
            if (jwk == null || jwk.getD() == null) {
                log.error("No private JWK key found for signing");
                throw new CustomException(ErrorCodes.INTERNAL_ERROR, "Unexpected error occured while signing the response. Unable to fetch the JCMS private key");
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

            // Convert payload to JSON
            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);

            // Sign JWT
            Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
            String fullJwt = JWT.create()
                    .withPayload(payloadJson)
                    .sign(algorithm);

            // Convert to detached JWT (remove payload part)
            String[] parts = fullJwt.split("\\.");
            if (parts.length != 3) {
                log.error("Unexpected JWT format while signing");
                throw new CustomException(ErrorCodes.INTERNAL_ERROR, "Unexpected JWT format while signing");
            }

            String detachedJwt = parts[0] + ".." + parts[2];
            log.info("Detached JWT generated successfully");
            return detachedJwt;

        } catch (Exception e) {
            log.error("Error while creating detached JWT:");
            throw new CustomException(ErrorCodes.INTERNAL_ERROR, "unable to sign the response: " + e.getMessage());
        }
    }

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
            log.error("Failed to decode certificate");
            return null;
        }
    }



}
