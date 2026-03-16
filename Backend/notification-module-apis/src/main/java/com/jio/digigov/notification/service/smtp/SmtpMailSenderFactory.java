package com.jio.digigov.notification.service.smtp;

import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.service.ConfigurationEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Factory for creating JavaMailSender instances based on SMTP configuration.
 * Creates dynamic mail senders per business configuration to support multi-tenant SMTP.
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // Create mail sender from business configuration
 * JavaMailSender mailSender = factory.createMailSender(notificationConfig);
 *
 * // Use mail sender to send emails
 * MimeMessage message = mailSender.createMimeMessage();
 * ...
 * mailSender.send(message);
 * </pre>
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpMailSenderFactory {

    private final ConfigurationEncryptionService encryptionService;

    /**
     * Create a JavaMailSender from NotificationConfig with SMTP settings.
     *
     * @param config The notification configuration containing SMTP details
     * @return Configured JavaMailSender instance
     * @throws IllegalArgumentException if configuration is invalid
     */
    public JavaMailSender createMailSender(NotificationConfig config) {
        if (config == null || config.getSmtpDetails() == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        var details = config.getSmtpDetails();

        if (details.getServerAddress() == null || details.getPort() == null) {
            throw new IllegalArgumentException(
                    "SMTP host and port are required for SMTP provider");
        }

        log.info("Creating JavaMailSender for businessId: {}, host: {}, port: {}",
                config.getBusinessId(), details.getServerAddress(), details.getPort());

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Basic SMTP configuration
        mailSender.setHost(details.getServerAddress());
        mailSender.setPort(details.getPort());

        // Authentication
        if (details.getUsername() != null) {
            mailSender.setUsername(details.getUsername());

            // Decrypt credentials if encrypted
            if (details.getPassword() != null) {
                String decryptedPassword = encryptionService.decryptIfEncrypted(
                        details.getPassword());
                mailSender.setPassword(decryptedPassword);
            }
        }

        // Default encoding
        mailSender.setDefaultEncoding("UTF-8");

        // SMTP properties
        Properties props = mailSender.getJavaMailProperties();

        // Transport protocol
        props.put("mail.transport.protocol", "smtp");

        // Authentication
        Boolean authEnabled = details.getSmtpAuthEnabled() != null ?
                details.getSmtpAuthEnabled() : true;
        props.put("mail.smtp.auth", authEnabled);

        // TLS/SSL configuration
        String tlsSslType = details.getTlsSsl() != null ?
                details.getTlsSsl() : "TLS";
        Boolean useTls = "TLS".equalsIgnoreCase(tlsSslType);
        Boolean useSsl = "SSL".equalsIgnoreCase(tlsSslType);

        if (useTls) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        if (useSsl) {
            props.put("mail.smtp.ssl.enable", "true");
            // If using SSL, typically port 465
            props.put("mail.smtp.socketFactory.port", details.getPort());
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        // Timeouts
        Integer connectionTimeout = details.getSmtpConnectionTimeout() != null ?
                details.getSmtpConnectionTimeout() : 5000;
        Integer socketTimeout = details.getSmtpSocketTimeout() != null ?
                details.getSmtpSocketTimeout() : 5000;

        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", socketTimeout);
        props.put("mail.smtp.writetimeout", socketTimeout);

        // Debug mode (can be enabled via config if needed)
        props.put("mail.debug", "false");

        // Additional recommended properties
        props.put("mail.smtp.ssl.trust", details.getServerAddress());
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        log.info("JavaMailSender created successfully for businessId: {} with auth={}, TLS={}, SSL={}",
                config.getBusinessId(), authEnabled, useTls, useSsl);

        return mailSender;
    }

    /**
     * Test SMTP connection by attempting to connect to the mail server.
     *
     * @param config The notification configuration
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection(NotificationConfig config) {
        try {
            JavaMailSenderImpl mailSender = (JavaMailSenderImpl) createMailSender(config);

            // Test connection
            mailSender.testConnection();

            log.info("SMTP connection test successful for businessId: {}", config.getBusinessId());
            return true;

        } catch (Exception e) {
            log.error("SMTP connection test failed for businessId: {}", config.getBusinessId());
            return false;
        }
    }
}
