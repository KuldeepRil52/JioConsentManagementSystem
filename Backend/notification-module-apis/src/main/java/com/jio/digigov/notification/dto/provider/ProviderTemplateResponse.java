package com.jio.digigov.notification.dto.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified template creation response for provider abstraction layer.
 *
 * @author Notification Service Team
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderTemplateResponse {

    /**
     * Success status
     */
    private Boolean success;

    /**
     * Provider-generated template ID
     * DigiGov: "TEMPL000006296347"
     * SMTP: UUID
     */
    private String templateId;

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
     * Transaction ID for tracking
     */
    private String transactionId;

    /**
     * Indicates if template requires approval
     */
    @Builder.Default
    private Boolean requiresApproval = false;

    /**
     * Create a success response
     */
    public static ProviderTemplateResponse success(String templateId, String message,
                                                    Boolean requiresApproval, String transactionId) {
        return ProviderTemplateResponse.builder()
                .success(true)
                .templateId(templateId)
                .message(message)
                .requiresApproval(requiresApproval)
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a failure response
     */
    public static ProviderTemplateResponse failure(String errorCode, String errorDescription, String transactionId) {
        return ProviderTemplateResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorDescription(errorDescription)
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
