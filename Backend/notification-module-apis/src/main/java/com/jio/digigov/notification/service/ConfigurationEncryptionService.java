package com.jio.digigov.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for encrypting and decrypting sensitive configuration data.
 * Uses Jasypt for AES-256 encryption of SMTP passwords and other sensitive fields.
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // Encrypt credentials before saving
 * String encryptedPassword = encryptionService.encrypt("myPassword123");
 *
 * // Decrypt credentials before using
 * String decryptedPassword = encryptionService.decrypt("ENC(...)");
 * </pre>
 *
 * <p><b>Configuration:</b></p>
 * Set the encryption key in environment variable: JASYPT_ENCRYPTOR_PASSWORD
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationEncryptionService {

    private final StringEncryptor stringEncryptor;

    private static final String ENCRYPTED_PREFIX = "ENC(";
    private static final String ENCRYPTED_SUFFIX = ")";

    /**
     * Encrypt a plaintext value.
     *
     * @param plaintext The value to encrypt
     * @return Encrypted value in format: ENC(encrypted_value)
     */
    public String encrypt(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            log.warn("Attempted to encrypt null or empty value");
            return plaintext;
        }

        // Don't encrypt if already encrypted
        if (isEncrypted(plaintext)) {
            log.debug("Value is already encrypted, skipping encryption");
            return plaintext;
        }

        try {
            String encrypted = stringEncryptor.encrypt(plaintext);
            String result = ENCRYPTED_PREFIX + encrypted + ENCRYPTED_SUFFIX;
            log.debug("Successfully encrypted value");
            return result;
        } catch (Exception e) {
            log.error("Failed to encrypt value");
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt an encrypted value.
     *
     * @param encryptedValue The encrypted value (with or without ENC() wrapper)
     * @return Decrypted plaintext value
     */
    public String decrypt(String encryptedValue) {
        if (!StringUtils.hasText(encryptedValue)) {
            log.warn("Attempted to decrypt null or empty value");
            return encryptedValue;
        }

        // If not encrypted, return as-is
        if (!isEncrypted(encryptedValue)) {
            log.debug("Value is not encrypted, returning as-is");
            return encryptedValue;
        }

        try {
            // Remove ENC() wrapper
            String unwrapped = unwrapEncryptedValue(encryptedValue);
            String decrypted = stringEncryptor.decrypt(unwrapped);
            log.debug("Successfully decrypted value");
            return decrypted;
        } catch (Exception e) {
            log.error("Failed to decrypt value");
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Check if a value is encrypted.
     *
     * @param value The value to check
     * @return true if encrypted (starts with ENC()), false otherwise
     */
    public boolean isEncrypted(String value) {
        return value != null &&
                value.startsWith(ENCRYPTED_PREFIX) &&
                value.endsWith(ENCRYPTED_SUFFIX);
    }

    /**
     * Remove ENC() wrapper from encrypted value.
     *
     * @param encryptedValue The encrypted value with ENC() wrapper
     * @return The encrypted value without wrapper
     */
    private String unwrapEncryptedValue(String encryptedValue) {
        if (!isEncrypted(encryptedValue)) {
            return encryptedValue;
        }

        return encryptedValue.substring(
                ENCRYPTED_PREFIX.length(),
                encryptedValue.length() - ENCRYPTED_SUFFIX.length()
        );
    }

    /**
     * Encrypt a value only if it's not already encrypted.
     *
     * @param value The value to encrypt
     * @return Encrypted value or original if already encrypted
     */
    public String encryptIfNotEncrypted(String value) {
        if (isEncrypted(value)) {
            return value;
        }
        return encrypt(value);
    }

    /**
     * Decrypt a value only if it's encrypted.
     *
     * @param value The value to decrypt
     * @return Decrypted value or original if not encrypted
     */
    public String decryptIfEncrypted(String value) {
        if (!isEncrypted(value)) {
            return value;
        }
        return decrypt(value);
    }
}
