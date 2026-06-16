package com.jio.digigov.notification.dto.masterlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.jio.digigov.notification.enums.MasterListDataSource;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the result of resolving a master list argument.
 *
 * This class captures both successful and failed resolution attempts, providing
 * detailed information for logging, debugging, and error handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedArgument {

    /**
     * The original argument key from the template (e.g., "<#ARG1>")
     */
    private String argumentKey;

    /**
     * The master label that was resolved (e.g., "MASTER_LABEL_USER_NAME")
     */
    private String masterLabel;

    /**
     * The data source used for resolution
     */
    private MasterListDataSource dataSource;

    /**
     * The resolved value (null if resolution failed)
     */
    private String resolvedValue;

    /**
     * Whether the resolution was successful
     */
    private boolean success;

    /**
     * Error message if resolution failed
     */
    private String errorMessage;

    /**
     * Additional details about the resolution process
     */
    private String resolutionDetails;

    /**
     * Timestamp when the resolution was attempted
     */
    private LocalDateTime resolvedAt;

    /**
     * Time taken to resolve this argument in milliseconds
     */
    private Long resolutionTimeMs;

    /**
     * Creates a successful resolution result.
     *
     * @param argumentKey the argument key
     * @param masterLabel the master label
     * @param dataSource the data source used
     * @param resolvedValue the resolved value
     * @param resolutionDetails additional details
     * @param resolutionTimeMs time taken in milliseconds
     * @return successful ResolvedArgument
     */
    public static ResolvedArgument success(String argumentKey, String masterLabel,
                                         MasterListDataSource dataSource, String resolvedValue,
                                         String resolutionDetails, Long resolutionTimeMs) {
        return ResolvedArgument.builder()
            .argumentKey(argumentKey)
            .masterLabel(masterLabel)
            .dataSource(dataSource)
            .resolvedValue(resolvedValue)
            .success(true)
            .resolutionDetails(resolutionDetails)
            .resolvedAt(LocalDateTime.now())
            .resolutionTimeMs(resolutionTimeMs)
            .build();
    }

    /**
     * Creates a failed resolution result.
     *
     * @param argumentKey the argument key
     * @param masterLabel the master label
     * @param dataSource the data source attempted
     * @param errorMessage the error message
     * @param resolutionTimeMs time taken in milliseconds
     * @return failed ResolvedArgument
     */
    public static ResolvedArgument failure(String argumentKey, String masterLabel,
                                         MasterListDataSource dataSource, String errorMessage,
                                         Long resolutionTimeMs) {
        return ResolvedArgument.builder()
            .argumentKey(argumentKey)
            .masterLabel(masterLabel)
            .dataSource(dataSource)
            .success(false)
            .errorMessage(errorMessage)
            .resolvedAt(LocalDateTime.now())
            .resolutionTimeMs(resolutionTimeMs)
            .build();
    }

    /**
     * Gets a human-readable summary of this resolution.
     *
     * @return summary string
     */
    public String getSummary() {
        if (success) {
            return String.format("✓ %s -> %s (%s) [%dms]",
                argumentKey, resolvedValue, dataSource, resolutionTimeMs);
        } else {
            return String.format("✗ %s -> FAILED: %s (%s) [%dms]",
                argumentKey, errorMessage, dataSource, resolutionTimeMs);
        }
    }
}