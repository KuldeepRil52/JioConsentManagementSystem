package com.jio.digigov.auditmodule.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for computing SHA-256 hashes in both
 * hexadecimal and Base64 formats.
 */
public final class HashUtils {

    private HashUtils() {
        // Prevent instantiation
    }

    /**
     * Computes a SHA-256 hash of the given input string
     * and returns it as a lowercase hexadecimal string.
     *
     * @param data input string to hash
     * @return SHA-256 hash in hex format
     */
    public static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // This should never happen with "SHA-256"
            throw new RuntimeException("Failed to compute SHA-256 hash", e);
        }
    }

    /**
     * Computes a SHA-256 hash and encodes it in Base64.
     *
     * @param data input string to hash
     * @return SHA-256 hash in Base64 format
     */
    public static String sha256Base64(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute SHA-256 hash (Base64)", e);
        }
    }
}