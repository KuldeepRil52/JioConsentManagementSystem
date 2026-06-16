package com.jio.digigov.notification.service.provider;

import com.jio.digigov.notification.dto.provider.*;
import com.jio.digigov.notification.entity.NotificationConfig;
import com.jio.digigov.notification.enums.NotificationChannel;
import com.jio.digigov.notification.enums.ProviderType;

import java.util.List;

/**
 * Unified notification provider service interface.
 * This interface provides abstraction for multiple notification providers
 * (DigiGov, SMTP, MSG91, AWS SNS, etc.)
 *
 * <p><b>Design Pattern:</b> Strategy Pattern</p>
 * <p>Different implementations provide different notification delivery mechanisms
 * while maintaining a consistent interface.</p>
 *
 * <p><b>Implementations:</b></p>
 * <ul>
 *   <li>{@link com.jio.digigov.notification.service.provider.impl.DigiGovProviderService} - DigiGov notification gateway</li>
 *   <li>{@link com.jio.digigov.notification.service.provider.impl.SmtpProviderService} - Direct SMTP email delivery</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // Get provider via factory
 * NotificationProviderService provider = providerFactory.getProvider(
 *     ProviderType.SMTP, NotificationChannel.EMAIL, config
 * );
 *
 * // Send email
 * ProviderEmailResponse response = provider.sendEmail(emailRequest, config);
 *
 * if (response.getSuccess()) {
 *     log.info("Email sent successfully: {}", response.getMessageId());
 * }
 * </pre>
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
public interface NotificationProviderService {

    /**
     * Get the provider type this service implements.
     *
     * @return Provider type (DIGIGOV, SMTP, etc.)
     */
    ProviderType getProviderType();

    /**
     * Get supported channels for this provider.
     *
     * @return List of supported notification channels
     */
    List<NotificationChannel> getSupportedChannels();

    /**
     * Check if this provider supports a specific channel.
     *
     * @param channel The notification channel to check
     * @return true if supported, false otherwise
     */
    boolean supportsChannel(NotificationChannel channel);

    /**
     * Check if this provider requires template approval.
     * DigiGov requires approval via Admin API, SMTP does not.
     *
     * @return true if approval required, false otherwise
     */
    boolean requiresApproval();

    /**
     * Onboard/create a new template with the provider.
     * For DigiGov: calls onboard API
     * For SMTP: generates UUID and validates template
     *
     * @param request Template creation request
     * @param config Provider configuration
     * @return Template creation response with templateId
     */
    ProviderTemplateResponse onboardTemplate(ProviderTemplateRequest request, NotificationConfig config);

    /**
     * Approve a template (if provider requires approval).
     * For DigiGov: calls approve API with ADMIN credentials
     * For SMTP: no-op, returns success immediately
     *
     * @param templateId Template ID to approve
     * @param config Provider configuration
     * @param transactionId Transaction ID for tracking
     * @return Approval response
     */
    ProviderTemplateResponse approveTemplate(String templateId, NotificationConfig config, String transactionId);

    /**
     * Send an email notification.
     *
     * @param request Email send request with all required details
     * @param config Provider configuration
     * @return Send response with status and tracking information
     */
    ProviderEmailResponse sendEmail(ProviderEmailRequest request, NotificationConfig config);

    /**
     * Validate provider configuration.
     * Checks if all required fields are present and valid.
     *
     * @param config Provider configuration to validate
     * @return true if valid, false otherwise
     */
    boolean validateConfiguration(NotificationConfig config);

    /**
     * Test provider connectivity.
     * For DigiGov: attempts token generation
     * For SMTP: attempts SMTP connection
     *
     * @param config Provider configuration
     * @return true if connection successful, false otherwise
     */
    boolean testConnection(NotificationConfig config);

    /**
     * Get provider-specific error code mapping.
     * Maps provider errors to standard notification system error codes.
     *
     * @param providerErrorCode Provider-specific error code
     * @return Standard error code
     */
    String mapErrorCode(String providerErrorCode);

    /**
     * Determine if an error is retryable.
     * Some errors (network issues, timeouts) should be retried,
     * others (invalid credentials, bad request) should not.
     *
     * @param errorCode Error code to check
     * @return true if retryable, false otherwise
     */
    boolean isRetryableError(String errorCode);
}
