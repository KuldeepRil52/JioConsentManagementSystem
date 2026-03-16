package com.jio.digigov.notification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class for OTP encryption and decryption using RSA public/private key cryptography.
 *
 * This utility provides secure encryption of OTP values before storing them in the database
 * and decryption during verification. It uses RSA-2048 encryption with OAEP padding (SHA-256).
 *
 * Security Features:
 * - RSA public-key cryptography for asymmetric encryption
 * - Public key used for encryption (during OTP generation)
 * - Private key used for decryption (during OTP verification)
 * - Base64 encoding for safe storage of encrypted values
 * - Configurable keys via application.yml
 *
 * Key Management:
 * - Keys are configured in application.yml under 'otp.encryption' section
 * - Public key: Base64-encoded X.509 format
 * - Private key: Base64-encoded PKCS#8 format
 * - Keys should be generated using standard RSA key generation tools
 *
 * Usage Example:
 * <pre>
 * // Encrypt OTP before saving to database
 * String plainOTP = "123456";
 * String encryptedOTP = otpEncryptionUtil.encrypt(plainOTP);
 *
 * // Decrypt OTP during verification
 * String decryptedOTP = otpEncryptionUtil.decrypt(encryptedOTP);
 * </pre>
 *
 * Key Generation (for reference):
 * <pre>
 * # Generate RSA private key
 * openssl genrsa -out private_key.pem 2048
 *
 * # Generate RSA public key from private key
 * openssl rsa -in private_key.pem -pubout -out public_key.pem
 *
 * # Convert to PKCS#8 format (for Java)
 * openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt
 *
 * # Convert to DER format for public key
 * openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der
 *
 * # Base64 encode for application.yml
 * base64 -i public_key.der -o public_key_base64.txt
 * base64 -i private_key.der -o private_key_base64.txt
 * </pre>
 *
 * @author Notification Service Team
 * @version 1.8.0
 * @since 2025-11-06
 */
@Component
@Slf4j
public class OTPEncryptionUtil {

    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final String ALGORITHM = "RSA";

    @Value("${otp.encryption.public-key:}")
    private String publicKeyString;

    @Value("${otp.encryption.private-key:}")
    private String privateKeyString;

    /**
     * Encrypts the OTP value using the RSA public key.
     *
     * This method uses the public key to encrypt the plaintext OTP value,
     * making it safe to store in the database. The encrypted value is
     * Base64-encoded for storage as a string.
     *
     * @param plainOTP Plain text OTP value to encrypt
     * @return Base64-encoded encrypted OTP value
     * @throws RuntimeException if encryption fails
     */
    public String encrypt(String plainOTP) {
        try {
            if (plainOTP == null || plainOTP.isEmpty()) {
                throw new IllegalArgumentException("OTP value cannot be null or empty");
            }

            if (publicKeyString == null || publicKeyString.isEmpty()) {
                log.warn("Public key not configured, returning plaintext OTP");
                return plainOTP;
            }

            PublicKey publicKey = getPublicKey(publicKeyString);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encryptedBytes = cipher.doFinal(plainOTP.getBytes(StandardCharsets.UTF_8));
            String encryptedValue = Base64.getEncoder().encodeToString(encryptedBytes);

            log.debug("Successfully encrypted OTP value");
            return encryptedValue;

        } catch (Exception e) {
            log.error("Failed to encrypt OTP: {}", e.getMessage());
            throw new RuntimeException("OTP encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts the encrypted OTP value using the RSA private key.
     *
     * This method uses the private key to decrypt the encrypted OTP value
     * retrieved from the database, returning the original plaintext value
     * for comparison during verification.
     *
     * @param encryptedOTP Base64-encoded encrypted OTP value
     * @return Plain text OTP value
     * @throws RuntimeException if decryption fails
     */
    public String decrypt(String encryptedOTP) {
        try {
            if (encryptedOTP == null || encryptedOTP.isEmpty()) {
                throw new IllegalArgumentException("Encrypted OTP value cannot be null or empty");
            }

            if (privateKeyString == null || privateKeyString.isEmpty()) {
                log.warn("Private key not configured, returning encrypted OTP as-is");
                return encryptedOTP;
            }

            PrivateKey privateKey = getPrivateKey(privateKeyString);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedOTP));
            String decryptedValue = new String(decryptedBytes, StandardCharsets.UTF_8);

            log.debug("Successfully decrypted OTP value");
            return decryptedValue;

        } catch (Exception e) {
            log.error("Failed to decrypt OTP: {}", e.getMessage());
            throw new RuntimeException("OTP decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a Base64-encoded public key string to a PublicKey object.
     *
     * @param base64PublicKey Base64-encoded public key (X.509 format)
     * @return PublicKey object
     * @throws Exception if key conversion fails
     */
    private PublicKey getPublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Converts a Base64-encoded private key string to a PrivateKey object.
     *
     * @param base64PrivateKey Base64-encoded private key (PKCS#8 format)
     * @return PrivateKey object
     * @throws Exception if key conversion fails
     */
    private PrivateKey getPrivateKey(String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Checks if encryption is enabled based on key configuration.
     *
     * @return true if both public and private keys are configured, false otherwise
     */
    public boolean isEncryptionEnabled() {
        return publicKeyString != null && !publicKeyString.isEmpty()
                && privateKeyString != null && !privateKeyString.isEmpty();
    }
}
