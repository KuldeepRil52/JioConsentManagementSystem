package com.jio.schedular.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Utils {

    public <T> T convertJsonStringToObject(String jsonString, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            log.error("Failed to convert JSON to object");
            log.debug("Underlying JSON parsing exception: " + e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> filterRequestParam(Map<String, Object> params, List<String> allowedParams) {
        if (params == null || params.isEmpty()) {
            return new HashMap<>();
        }

        return params.entrySet().stream()
                .filter(entry -> allowedParams.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * Compute SHA-256 hash of a string
     *
     * @param input The string to hash
     * @return SHA-256 hash as hexadecimal string
     */
    public String computeSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert bytes to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found, error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to compute SHA-256 hash", e);
        }
    }
}