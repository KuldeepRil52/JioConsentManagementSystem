package com.jio.digigov.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for master list resolution.
 *
 * These properties control how master list argument resolution behaves,
 * including whether to use default values when resolution fails.
 */
@Data
@Component
@ConfigurationProperties(prefix = "master-list")
public class MasterListProperties {

    /**
     * Configuration loading settings.
     */
    private Config config = new Config();

    /**
     * Resolution settings.
     */
    private Resolution resolution = new Resolution();

    @Data
    public static class Config {
        /**
         * Path to the master list configuration file.
         */
        private String filePath = "master-list-config.json";

        /**
         * Database collection name for tenant-specific master list configurations.
         */
        private String databaseCollection = "master_list_config";

        /**
         * Cache duration for loaded master list configurations.
         */
        private Duration cacheDuration = Duration.ofMinutes(5);
    }

    @Data
    public static class Resolution {
        /**
         * Whether to fail fast when any argument resolution fails.
         * When false, allows using default values.
         */
        private boolean failFast = false;

        /**
         * Maximum time allowed for resolving all arguments.
         */
        private long maxResolutionTime = 10000;

        /**
         * Whether to batch database queries for improved performance.
         */
        private boolean batchDbQueries = true;

        /**
         * Whether to use default values when argument resolution fails.
         * When true, uses defaultValue from master list entry instead of throwing exception.
         * When false, throws exception on resolution failure (original behavior).
         */
        private boolean useDefaultValues = true;
    }
}