package com.jio.digigov.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for System Registry service integration.
 * Used for consent withdrawal API calls during callback processing.
 */
@Data
@Component
@ConfigurationProperties(prefix = "system-registry.service")
public class SystemRegistryProperties {

    /**
     * Base URL for the System Registry service.
     */
    private String baseUrl = "http://10.173.184.32:30011";

    /**
     * API endpoints configuration.
     */
    private Endpoints endpoints = new Endpoints();

    @Data
    public static class Endpoints {
        /**
         * Consent withdraw endpoint path.
         * The {consentId} placeholder will be replaced at runtime.
         */
        private String consentWithdraw = "/registry/api/v1/consents/{consentId}/withdraw";
    }
}
