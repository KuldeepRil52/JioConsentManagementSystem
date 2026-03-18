package com.jio.partnerportal.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Utils {

    public Map<String, String> filterRequestParam(Map<String, String> params, List<String> allowedParams) {
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

    public static String generateClientId(String panNumber) {
        // 1. Prefix
        String prefix = "CLT";
        // 2. Date segment (YYMM)
        String dateSegment = new java.text.SimpleDateFormat("yyMM").format(new java.util.Date());
        // 3. Random 4-character alphanumeric
        String randomSegment = getRandomAlphaNumeric(4);
        // 4. Optional short hash from PAN (to map internally)
        String hashSegment = Integer.toHexString(panNumber.hashCode()).toUpperCase();
        hashSegment = hashSegment.substring(0, 1); // take 1 char for brevity
        // 5. Final Client ID
        return prefix + dateSegment + randomSegment + hashSegment;
    }


    private static String getRandomAlphaNumeric(int count) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom rnd = new SecureRandom();
        for (int i = 0; i < count; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String generateRandomAlphaNumeric(int count) {
        return getRandomAlphaNumeric(count);
    }

    /**
     * Generates wso2OnboardName with format: name + " " + randomString
     * Total length (name + randomString, excluding space) will be less than 25 characters
     * @param name The base name
     * @return wso2OnboardName with appropriate random string length
     */
    public static String generateWso2OnboardName(String name) {
        int nameLength = name.length();
        int maxTotalLength = 24; // less than 25 characters (excluding space)
        
        // Calculate available length for random string (space is not counted)
        int availableLength = maxTotalLength - nameLength;
        
        // Use minimum of 10 or available length, but at least 1 character
        // If name is 20 chars: 24 - 20 = 4 available → uses 4 random chars
        // If name is 12 chars: 24 - 12 = 12 available → uses 10 random chars (max)
        int randomStringLength = Math.max(1, Math.min(10, availableLength));
        
        String randomString = generateRandomAlphaNumeric(randomStringLength);
        return name + " " + randomString;
    }
    
    public static boolean isValidBase64(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        String trimmed = str.trim();

        if (trimmed.length() % 4 != 0) {
            return false;
        }

        if (!trimmed.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            return false;
        }

        try {
            Base64.getDecoder().decode(trimmed);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
