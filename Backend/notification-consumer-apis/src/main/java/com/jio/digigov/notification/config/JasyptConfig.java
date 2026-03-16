package com.jio.digigov.notification.config;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jasypt encryption configuration for securing sensitive data.
 * Provides AES-256 encryption for SMTP credentialss and other sensitive fields.
 *
 * <p><b>Environment Variables:</b></p>
 * <ul>
 *   <li>JASYPT_ENCRYPTOR_PASSWORD - Master encryption key (required)</li>
 * </ul>
 *
 * <p><b>Security Notes:</b></p>
 * <ul>
 *   <li>Never commit the encryption credentials to version control</li>
 *   <li>Use different keys for different environments</li>
 *   <li>Rotate keys periodically</li>
 *   <li>Store keys in secure secret management systems (Vault, AWS Secrets Manager, etc.)</li>
 * </ul>
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Configuration
@EnableEncryptableProperties
public class JasyptConfig {

    @Value("${jasypt.encryptor.credentials:#{environment.JASYPT_ENCRYPTOR_PASSWORD}}")
    private String encryptorPassword;

    /**
     * Configure the string encryptor bean for Jasypt.
     * Uses PBEWITHHMACSHA512ANDAES_256 algorithm for strong encryption.
     *
     * @return Configured string encryptor
     */
    @Primary
    @Bean(name = "jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        // Encryption algorithm - AES 256
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");

        // Master credentials from environment
        config.setPassword(encryptorPassword != null ? encryptorPassword : "defaultPassword");

        // Key obtention iterations (higher = more secure but slower)
        config.setKeyObtentionIterations("1000");

        // Pool size for parallel encryption/decryption
        config.setPoolSize("1");

        // Provider
        config.setProviderName("SunJCE");

        // Salt generator
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");

        // IV generator for AES
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");

        // String output type
        config.setStringOutputType("base64");

        encryptor.setConfig(config);

        return encryptor;
    }
}
