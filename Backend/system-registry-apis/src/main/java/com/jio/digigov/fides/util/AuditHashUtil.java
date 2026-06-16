package com.jio.digigov.fides.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class AuditHashUtil {

    private AuditHashUtil() {
        // utility class – prevent instantiation
    }

    /**
     * Generates a SHA-256 hash for audit-safe storage.
     *
     * @param input the input string (e.g. dataset YAML)
     * @return hex-encoded SHA-256 hash
     */
    public static String sha256(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // This should never happen in a standard JVM
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}