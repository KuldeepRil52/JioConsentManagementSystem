package com.jio.digigov.notification.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for callback purge SLA settings.
 *
 * Loads SLA configuration from application.yml under the prefix:
 * notification.callback.purge.sla
 *
 * Properties:
 * - hours: Number of hours within which a callback should be purged after acknowledgement
 *
 * Example configuration in application.yml:
 * <pre>
 * notification:
 *   callback:
 *     purge:
 *       sla:
 *         hours: 24
 * </pre>
 *
 * Can be overridden via environment variable:
 * NOTIFICATION_CALLBACK_PURGE_SLA_HOURS=48
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "notification.callback.purge.sla")
@Data
@Validated
public class SlaConfiguration {

    /**
     * Number of hours within which a callback should be purged after acknowledgement.
     * Default: 24 hours
     * Minimum: 1 hour
     *
     * A callback is considered:
     * - Pending: If ACKNOWLEDGED but not DELETED, and time since acknowledgement < SLA hours
     * - Overdue: If ACKNOWLEDGED but not DELETED, and time since acknowledgement >= SLA hours
     * - Purged: If ACKNOWLEDGED then DELETED in statusHistory (regardless of timing)
     */
    @Min(value = 1, message = "SLA hours must be at least 1 hour")
    private int hours = 24;

    /**
     * Gets the SLA threshold in hours.
     *
     * @return SLA hours
     */
    public int getHours() {
        return hours;
    }

    /**
     * Sets the SLA threshold in hours.
     *
     * @param hours SLA hours (must be >= 1)
     */
    public void setHours(int hours) {
        this.hours = hours;
    }
}
