package com.jio.auth.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.auth.constants.ErrorCode;
import com.jio.auth.exception.CustomException;
import com.jio.auth.model.JwkKey;
import com.jio.auth.repository.JwkKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.security.spec.RSAPrivateCrtKeySpec;


@Slf4j
@Component
public class KeyProvider {



    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JwkKeyRepository jwkKeyRepository;

    public JsonNode getPublicCertFromDb() {
        log.debug("Fetching first available JWK from DB");
        try {
            JwkKey jwk = jwkKeyRepository.findFirstByOrderByKidAsc();
            if (jwk == null) {
                throw new CustomException(ErrorCode.NOT_FOUND, "No JWK found in database");
            }
            String keyJson = new ObjectMapper().writeValueAsString(jwk);
            JsonNode json = mapper.readTree(keyJson);
            log.debug("Successfully fetched and parsed JWK from DB");
            return json;
        } catch (Exception e) {
            log.error("Failed to fetch JWK from DB", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Error loading key from DB");
        }
    }

    public JsonNode getPublicCert(String fileName) {
        log.debug("Loading public certificate JSON from file: {}", fileName);
        try (InputStream is = new ClassPathResource(fileName).getInputStream()) {
            JsonNode json = mapper.readTree(is);
            log.debug("Successfully loaded public certificate JSON from file by: getPublicCert()");
            return json;
        } catch (IOException e) {
            log.error("Failed to load public certificate JSON from file by: getPublicCert()");
            throw new RuntimeException("Failed to load public certificate JSON from file  by: getPublicCert() ");
        }
    }

    public PublicKey getPublicKeyFromJson(String jsonContent) {
        log.debug("Generating PublicKey from JSON content");
        try {
            JwkKey jwk = jwkKeyRepository.findFirstByOrderByKidAsc();

            String n = jwk.getN();
            String e = jwk.getE();

            log.debug("Modulus (n) and Exponent (e) extracted from JSON");

            byte[] modulusBytes = Base64.getUrlDecoder().decode(n);
            byte[] exponentBytes = Base64.getUrlDecoder().decode(e);

            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);

            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            log.debug("Successfully generated RSA PublicKey from JSON");
            return publicKey;
        } catch (Exception ex) {
            log.error("Failed to generate PublicKey from JSON by: getPublicKeyFromJson()");
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Error during public key retrieval");
        }
    }
    public PrivateKey getPrivateKeyFromJson(String jsonContent) {
        log.debug("Generating PrivateKey from JSON content");
        try {
            JwkKey jwk = jwkKeyRepository.findFirstByOrderByKidAsc();

            String n = jwk.getN();
            String e = jwk.getE();
            String d = jwk.getD();
            String p = jwk.getP();
            String q = jwk.getQ();
            String dp = jwk.getDp();
            String dq = jwk.getDq();
            String qi = jwk.getQi();

            Base64.Decoder urlDecoder = Base64.getUrlDecoder();
            BigInteger modulus = new BigInteger(1, urlDecoder.decode(n));
            BigInteger publicExponent = new BigInteger(1, urlDecoder.decode(e));
            BigInteger privateExponent = new BigInteger(1, urlDecoder.decode(d));
            BigInteger primeP = new BigInteger(1, urlDecoder.decode(p));
            BigInteger primeQ = new BigInteger(1, urlDecoder.decode(q));
            BigInteger primeExponentP = new BigInteger(1, urlDecoder.decode(dp));
            BigInteger primeExponentQ = new BigInteger(1, urlDecoder.decode(dq));
            BigInteger crtCoefficient = new BigInteger(1, urlDecoder.decode(qi));

            RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
                    modulus,
                    publicExponent,
                    privateExponent,
                    primeP,
                    primeQ,
                    primeExponentP,
                    primeExponentQ,
                    crtCoefficient
            );

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            return privateKey;

        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Failed to generate PrivateKey from JSON by: getPrivateKeyFromJson()");
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Error during private key retrieval");
        }
    }


}

