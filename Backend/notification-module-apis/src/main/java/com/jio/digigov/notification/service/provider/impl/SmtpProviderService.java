package com.jio.digigov.notification.service.provider.impl;

import com.jio.digigov.notification.dto.provider.*;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.ProviderType;
import com.jio.digigov.notification.service.provider.NotificationProviderService;
import com.jio.digigov.notification.service.smtp.SmtpMailSenderFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * SMTP provider implementation for direct email delivery.
 * Supports HTML and plain text emails without requiring external approval.
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Direct SMTP email sending (no DigiGov dependency)</li>
 *   <li>HTML and plain text support</li>
 *   <li>No template approval required</li>
 *   <li>Multi-tenant SMTP configuration</li>
 *   <li>Dynamic mail sender creation per business</li>
 * </ul>
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpProviderService implements NotificationProviderService {

    private final SmtpMailSenderFactory mailSenderFactory;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.SMTP;
    }

    @Override
    public List<NotificationChannel> getSupportedChannels() {
        return List.of(NotificationChannel.EMAIL); // SMTP only supports EMAIL
    }

    @Override
    public boolean supportsChannel(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public boolean requiresApproval() {
        return false; // SMTP templates don't require external approval
    }

    @Override
    public ProviderTemplateResponse onboardTemplate(ProviderTemplateRequest request, NotificationConfig config) {
        try {
            log.info("Onboarding SMTP template for eventType: {}", request.getEventType());

            // For SMTP, we don't need to onboard to an external service
            // Just generate a UUID as templateId and validate the template
            String templateId = "SMTP-" + UUID.randomUUID().toString();

            // Validate template content
            if (request.getChannelType() == NotificationChannel.EMAIL) {
                if (!StringUtils.hasText(request.getEmailSubject()) ||
                        !StringUtils.hasText(request.getEmailBody())) {
                    return ProviderTemplateResponse.failure(
                            "INVALID_TEMPLATE",
                            "Email subject and body are required for SMTP templates",
                            request.getTransactionId()
                    );
                }
            }

            log.info("Successfully created SMTP template: {}", templateId);
            return ProviderTemplateResponse.success(
                    templateId,
                    "SMTP template created successfully (no external onboarding required)",
                    false, // SMTP doesn't require approval
                    request.getTransactionId()
            );

        } catch (Exception e) {
            log.error("Error creating SMTP template");
            return ProviderTemplateResponse.failure(
                    "ONBOARD_ERROR",
                    "Error creating SMTP template: " + e.getMessage(),
                    request.getTransactionId()
            );
        }
    }

    @Override
    public ProviderTemplateResponse approveTemplate(String templateId, NotificationConfig config,
                                                     String transactionId) {
        // SMTP templates don't require approval - return success immediately
        log.info("SMTP template approval requested for: {} (no-op, auto-approved)", templateId);

        return ProviderTemplateResponse.success(
                templateId,
                "SMTP template auto-approved (no approval required)",
                false,
                transactionId
        );
    }

    @Override
    public ProviderEmailResponse sendEmail(ProviderEmailRequest request, NotificationConfig config) {
        try {
            log.info("Sending email via SMTP for template: {}, to: {}",
                    request.getTemplateId(), request.getTo());

            // Validate request
            if (request.getTo() == null || request.getTo().isEmpty()) {
                return ProviderEmailResponse.failure(
                        "INVALID_REQUEST",
                        "Recipient email addresses are required",
                        request.getTransactionId(),
                        false
                );
            }

            if (!StringUtils.hasText(request.getSubject()) || !StringUtils.hasText(request.getBody())) {
                return ProviderEmailResponse.failure(
                        "INVALID_REQUEST",
                        "Email subject and body are required",
                        request.getTransactionId(),
                        false
                );
            }

            // Create mail sender for this business configuration
            JavaMailSender mailSender = mailSenderFactory.createMailSender(config);

            // Create MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set recipients
            helper.setTo(request.getTo().toArray(new String[0]));

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().toArray(new String[0]));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().toArray(new String[0]));
            }

            // Set from address
            String fromEmail = request.getFromEmail() != null ?
                    request.getFromEmail() :
                    (config.getSmtpDetails() != null ? config.getSmtpDetails().getFromEmail() : null);

            String fromName = request.getFromName() != null ?
                    request.getFromName() :
                    (config.getSmtpDetails() != null ? config.getSmtpDetails().getSenderDisplayName() : null);

            if (fromName != null) {
                helper.setFrom(fromEmail, fromName);
            } else {
                helper.setFrom(fromEmail);
            }

            // Set subject and body
            helper.setSubject(request.getSubject());

            // Determine email type (HTML or TEXT)
            boolean isHtml = "HTML".equalsIgnoreCase(request.getEmailType());
            helper.setText(request.getBody(), isHtml);

            // Send email
            mailSender.send(message);

            // Generate message ID for tracking
            String messageId = "SMTP-MSG-" + UUID.randomUUID().toString();

            log.info("Successfully sent email via SMTP: messageId={}, to={}", messageId, request.getTo());

            return ProviderEmailResponse.success(
                    messageId,
                    "Email sent successfully via SMTP",
                    request.getTransactionId()
            );

        } catch (MessagingException e) {
            log.error("Messaging error sending email via SMTP");
            return ProviderEmailResponse.failure(
                    "MESSAGING_ERROR",
                    "Error creating email message: " + e.getMessage(),
                    request.getTransactionId(),
                    false // Messaging errors are typically not retryable
            );

        } catch (Exception e) {
            log.error("Error sending email via SMTP");

            // Determine if error is retryable
            boolean retryable = isRetryableError(e.getClass().getSimpleName());

            return ProviderEmailResponse.failure(
                    "SEND_ERROR",
                    "Error sending email: " + e.getMessage(),
                    request.getTransactionId(),
                    retryable
            );
        }
    }

    @Override
    public boolean validateConfiguration(NotificationConfig config) {
        if (config == null || config.getSmtpDetails() == null) {
            log.error("Configuration or smtpDetails is null");
            return false;
        }

        var smtpDetails = config.getSmtpDetails();

        boolean valid = StringUtils.hasText(smtpDetails.getServerAddress()) &&
                smtpDetails.getPort() != null &&
                StringUtils.hasText(smtpDetails.getFromEmail());

        if (!valid) {
            log.error("SMTP configuration validation failed: missing required fields " +
                    "(serverAddress, port, or fromEmail)");
        }

        // Optional: Validate email format
        if (valid && !isValidEmail(smtpDetails.getFromEmail())) {
            log.error("SMTP configuration validation failed: invalid fromEmail format");
            return false;
        }

        return valid;
    }

    @Override
    public boolean testConnection(NotificationConfig config) {
        try {
            log.info("Testing SMTP connection for businessId: {}", config.getBusinessId());

            // Test connection using factory
            boolean success = mailSenderFactory.testConnection(config);

            if (success) {
                log.info("SMTP connection test successful for businessId: {}", config.getBusinessId());
            } else {
                log.error("SMTP connection test failed for businessId: {}", config.getBusinessId());
            }

            return success;

        } catch (Exception e) {
            log.error("SMTP connection test failed for businessId: {}", config.getBusinessId());
            return false;
        }
    }

    @Override
    public String mapErrorCode(String providerErrorCode) {
        if (providerErrorCode == null) {
            return "UNKNOWN_ERROR";
        }

        return switch (providerErrorCode.toUpperCase()) {
            case "AUTHENTICATIONFAILEDEXCEPTION" -> "AUTHENTICATION_ERROR";
            case "SENDMESSAGEFAILEDEXCEPTION" -> "SEND_FAILED";
            case "MESSAGINGEXCEPTION" -> "MESSAGING_ERROR";
            case "MAILSENDEXCEPTION" -> "SEND_FAILED";
            case "CONNECTEXCEPTION", "SOCKETTIMEOUTEXCEPTION" -> "NETWORK_ERROR";
            default -> providerErrorCode;
        };
    }

    @Override
    public boolean isRetryableError(String errorCode) {
        if (errorCode == null) {
            return false;
        }

        String code = errorCode.toUpperCase();
        return code.contains("TIMEOUT") ||
                code.contains("NETWORK") ||
                code.contains("CONNECT") ||
                code.contains("SOCKET") ||
                code.contains("IO") ||
                code.equals("SEND_FAILED");
    }

    /**
     * Simple email validation.
     *
     * @param email Email address to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }

        // Simple regex for email validation
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
