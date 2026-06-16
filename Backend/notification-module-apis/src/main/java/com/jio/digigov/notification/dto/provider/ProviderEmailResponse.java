package com.jio.digigov.notification.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Unified email response DTO for provider abstraction layer.
 * This DTO standardizes responses from all notification providers.
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderEmailResponse {

    /**
     * Success status
     */
    private Boolean success;

    /**
     * Provider-specific message ID or tracking ID
     */
    private String messageId;

    /**
     * Response message
     */
    private String message;

    /**
     * Error code (if failed)
     */
    private String errorCode;

    /**
     * Error description (if failed)
     */
    private String errorDescription;

    /**
     * Response timestamp
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Provider-specific response data
     */
    private Map<String, Object> providerResponse;

    /**
     * Transaction ID for tracking
     */
    private String transactionId;

    /**
     * HTTP status code (if applicable)
     */
    private Integer httpStatusCode;

    /**
     * Retry recommended flag
     */
    @Builder.Default
    private Boolean retryRecommended = false;

    /**
     * Create a success response
     */
    public static ProviderEmailResponse success(String messageId, String message, String transactionId) {
        return ProviderEmailResponse.builder()
                .success(true)
                .messageId(messageId)
                .message(message)
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a failure response
     */
    public static ProviderEmailResponse failure(String errorCode, String errorDescription,
                                                 String transactionId, Boolean retryRecommended) {
        return ProviderEmailResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .transactionId(transactionId)
                .retryRecommended(retryRecommended)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
