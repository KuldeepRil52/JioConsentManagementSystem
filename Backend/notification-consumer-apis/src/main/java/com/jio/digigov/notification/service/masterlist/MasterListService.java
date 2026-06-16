package com.jio.digigov.notification.service.masterlist;

import com.jio.digigov.notification.dto.masterlist.MasterListConfig;
import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.dto.masterlist.ResolvedArgument;
import com.jio.digigov.notification.dto.request.event.TriggerEventRequestDto;
import com.jio.digigov.notification.enums.MasterListDataSource;
import com.jio.digigov.notification.service.masterlist.resolver.DatabaseResolver;
import com.jio.digigov.notification.service.masterlist.resolver.PayloadResolver;
import com.jio.digigov.notification.service.masterlist.resolver.TokenResolver;
import com.jio.digigov.notification.service.masterlist.resolver.ValueGenerator;
import com.jio.digigov.notification.config.MasterListProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main service for resolving master list arguments to their actual values.
 *
 * This service orchestrates the resolution of template arguments by:
 * 1. Loading master list configuration (file + database override)
 * 2. Validating that all required arguments can be resolved
 * 3. Delegating resolution to appropriate resolvers based on data source
 * 4. Batching database queries for optimal performance
 * 5. Replacing placeholders in template text with resolved values
 *
 * Resolution Flow:
 * 1. Load configuration for tenant/business
 * 2. Group arguments by data source type
 * 3. Batch resolve arguments per data source
 * 4. Validate all resolutions succeeded (fail-fast approach)
 * 5. Replace placeholders in templates
 *
 * Error Strategy: Fail-fast - if any argument fails to resolve, the entire
 * notification processing fails with a detailed error response.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterListService {

    private final MasterListConfigLoader configLoader;
    private final PayloadResolver payloadResolver;
    private final TokenResolver tokenResolver;
    private final DatabaseResolver databaseResolver;
    private final ValueGenerator valueGenerator;
    private final MasterListProperties masterListProperties;

    /**
     * Resolves all template arguments and replaces placeholders in template strings.
     *
     * This is the main entry point for template argument resolution. It handles:
     * - Loading configuration
     * - Validating required fields
     * - Resolving all arguments
     * - Replacing placeholders in template strings
     *
     * @param argumentsMap map of template placeholders to master labels
     * @param templateTexts map of template field names to template content
     * @param request the trigger event request
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return map of template field names to resolved content
     * @throws MasterListResolutionException if any argument fails to resolve
     */
    public Map<String, String> resolveAndReplaceTemplates(Map<String, String> argumentsMap,
                                                          Map<String, String> templateTexts,
                                                          TriggerEventRequestDto request,
                                                          String tenantId,
                                                          String businessId) {
        return resolveAndReplaceTemplates(argumentsMap, templateTexts, request, tenantId, businessId, null);
    }

    /**
     * Resolves all template arguments and replaces placeholders in template strings with headers support.
     *
     * This is the main entry point for template argument resolution. It handles:
     * - Loading configuration
     * - Validating required fields
     * - Resolving all arguments
     * - Replacing placeholders in template strings
     *
     * @param argumentsMap map of template placeholders to master labels
     * @param templateTexts map of template field names to template content
     * @param request the trigger event request
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @param headers optional map of HTTP headers (for header.* path resolution)
     * @return map of template field names to resolved content
     * @throws MasterListResolutionException if any argument fails to resolve
     */
    public Map<String, String> resolveAndReplaceTemplates(Map<String, String> argumentsMap,
                                                          Map<String, String> templateTexts,
                                                          TriggerEventRequestDto request,
                                                          String tenantId,
                                                          String businessId,
                                                          Map<String, String> headers) {
        if (argumentsMap == null || argumentsMap.isEmpty()) {
            log.debug("No arguments to resolve, returning original templates");
            return new HashMap<>(templateTexts);
        }

        if (templateTexts == null || templateTexts.isEmpty()) {
            log.debug("No template texts provided");
            return new HashMap<>();
        }

        log.info("Resolving {} arguments and replacing in {} templates for tenantId={}, businessId={}",
                argumentsMap.size(), templateTexts.size(), tenantId, businessId);

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Resolve all arguments
            Map<String, String> resolvedValues = resolveArguments(argumentsMap, request, tenantId, businessId, headers);

            // Step 2: Replace placeholders in all template texts
            Map<String, String> resolvedTemplates = new HashMap<>();
            for (Map.Entry<String, String> templateEntry : templateTexts.entrySet()) {
                String fieldName = templateEntry.getKey();
                String templateText = templateEntry.getValue();

                String resolvedTemplate = replacePlaceholders(templateText, resolvedValues);
                resolvedTemplates.put(fieldName, resolvedTemplate);

                log.debug("Resolved template field '{}': {} placeholders replaced", fieldName, argumentsMap.size());
            }

            long endTime = System.currentTimeMillis();
            log.info("Successfully resolved and replaced {} arguments in {} templates [{}ms]",
                    argumentsMap.size(), templateTexts.size(), endTime - startTime);

            return resolvedTemplates;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("Failed to resolve template arguments after {}ms: {}", endTime - startTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Resolves template arguments to their actual values.
     *
     * @param argumentsMap map of argument keys to master labels
     * @param request the trigger event request
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @return map of argument keys to resolved values
     * @throws MasterListResolutionException if resolution fails
     */
    public Map<String, String> resolveArguments(Map<String, String> argumentsMap,
                                               TriggerEventRequestDto request,
                                               String tenantId,
                                               String businessId) {
        return resolveArguments(argumentsMap, request, tenantId, businessId, null);
    }

    /**
     * Resolves template arguments to their actual values with headers support.
     *
     * @param argumentsMap map of argument keys to master labels
     * @param request the trigger event request
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @param headers optional map of HTTP headers (for header.* path resolution)
     * @return map of argument keys to resolved values
     * @throws MasterListResolutionException if resolution fails
     */
    public Map<String, String> resolveArguments(Map<String, String> argumentsMap,
                                               TriggerEventRequestDto request,
                                               String tenantId,
                                               String businessId,
                                               Map<String, String> headers) {
        if (argumentsMap == null || argumentsMap.isEmpty()) {
            return new HashMap<>();
        }

        log.debug("Resolving {} arguments for tenantId={}, businessId={}",
                 argumentsMap.size(), tenantId, businessId);

        try {
            // Step 1: Load master list configuration
            MasterListConfig config = configLoader.loadConfiguration(tenantId, businessId);
            log.debug("Loaded master list configuration with {} entries from {}",
                     config.size(), config.getSource());

            // Step 2: Validate required fields are present in request
            validateRequiredFields(argumentsMap, config, request);

            // Step 3: Group arguments by data source for batch processing
            Map<MasterListDataSource, Map<String, MasterListEntry>> argumentsBySource =
                groupArgumentsByDataSource(argumentsMap, config);

            // Step 4: Resolve arguments by data source
            Map<String, ResolvedArgument> resolutionResults = new HashMap<>();

            for (Map.Entry<MasterListDataSource, Map<String, MasterListEntry>> sourceGroup : argumentsBySource.entrySet()) {
                MasterListDataSource dataSource = sourceGroup.getKey();
                Map<String, MasterListEntry> entries = sourceGroup.getValue();

                log.debug("Resolving {} arguments from {} data source", entries.size(), dataSource);

                Map<String, ResolvedArgument> sourceResults = resolveArgumentsFromSource(
                    dataSource, entries, request, tenantId, businessId, headers);
                resolutionResults.putAll(sourceResults);
            }

            // Step 5: Validate all resolutions succeeded or apply default values
            validateAllResolutionsSucceeded(resolutionResults, argumentsMap, config);

            // Step 6: Extract resolved values
            Map<String, String> resolvedValues = resolutionResults.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getResolvedValue()
                ));

            log.info("Successfully resolved {}/{} arguments",
                    resolutionResults.values().stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum(),
                    argumentsMap.size());

            return resolvedValues;

        } catch (MasterListResolutionException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            log.error("Unexpected error during argument resolution: {}", e.getMessage());
            throw new MasterListResolutionException("Unexpected error during argument resolution", e);
        }
    }

    /**
     * Validates that all required fields for argument resolution are present in the request.
     *
     * @param argumentsMap map of arguments to resolve
     * @param config the master list configuration
     * @param request the trigger event request
     * @throws MasterListResolutionException if required fields are missing
     */
    private void validateRequiredFields(Map<String, String> argumentsMap,
                                      MasterListConfig config,
                                      TriggerEventRequestDto request) {
        List<String> missingFields = new ArrayList<>();

        for (Map.Entry<String, String> argEntry : argumentsMap.entrySet()) {
            String masterLabel = argEntry.getValue();
            MasterListEntry listEntry = config.getEntry(masterLabel);

            if (listEntry == null) {
                throw new MasterListResolutionException(
                    String.format("Master label '%s' not found in configuration", masterLabel));
            }

            // Check required fields based on data source
            switch (listEntry.getDataSource()) {
                case TOKEN -> {
                    if (request.getEventPayload() == null ||
                        request.getEventPayload().get("token") == null) {
                        missingFields.add("eventPayload.token");
                    }
                }
                case PAYLOAD -> {
                    // Payload validation is handled by the PayloadResolver
                    // We'll let it fail gracefully during resolution
                }
                case DB -> {
                    // Database validation will happen during query execution
                }
                case GENERATE -> {
                    // Generator validation is handled by ValueGenerator
                }
            }
        }

        if (!missingFields.isEmpty()) {
            throw new MasterListResolutionException(
                String.format("Missing required fields for argument resolution: %s", missingFields));
        }
    }

    /**
     * Groups arguments by their data source for batch processing.
     *
     * @param argumentsMap map of arguments to master labels
     * @param config the master list configuration
     * @return map of data sources to argument entries
     */
    private Map<MasterListDataSource, Map<String, MasterListEntry>> groupArgumentsByDataSource(
            Map<String, String> argumentsMap, MasterListConfig config) {

        Map<MasterListDataSource, Map<String, MasterListEntry>> grouped = new HashMap<>();

        for (Map.Entry<String, String> argEntry : argumentsMap.entrySet()) {
            String argumentKey = argEntry.getKey();
            String masterLabel = argEntry.getValue();

            MasterListEntry listEntry = config.getEntry(masterLabel);
            if (listEntry == null) {
                log.warn("Master label '{}' not found in configuration for argument '{}'",
                        masterLabel, argumentKey);
                continue;
            }

            grouped.computeIfAbsent(listEntry.getDataSource(), k -> new HashMap<>())
                   .put(argumentKey, listEntry);
        }

        return grouped;
    }

    /**
     * Resolves arguments from a specific data source.
     *
     * @param dataSource the data source type
     * @param entries the entries to resolve
     * @param request the trigger event request
     * @param tenantId the tenant identifier
     * @param businessId the business identifier
     * @param headers optional map of HTTP headers (for header.* path resolution)
     * @return map of argument keys to resolution results
     */
    private Map<String, ResolvedArgument> resolveArgumentsFromSource(MasterListDataSource dataSource,
                                                                   Map<String, MasterListEntry> entries,
                                                                   TriggerEventRequestDto request,
                                                                   String tenantId,
                                                                   String businessId,
                                                                   Map<String, String> headers) {
        Map<String, ResolvedArgument> results = new HashMap<>();
        long sourceStartTime = System.currentTimeMillis();

        try {
            switch (dataSource) {
                case PAYLOAD -> {
                    for (Map.Entry<String, MasterListEntry> entry : entries.entrySet()) {
                        results.put(entry.getKey(), resolvePayloadArgument(entry.getKey(), entry.getValue(), request, headers));
                    }
                }
                case TOKEN -> {
                    for (Map.Entry<String, MasterListEntry> entry : entries.entrySet()) {
                        results.put(entry.getKey(), resolveTokenArgument(entry.getKey(), entry.getValue(), request));
                    }
                }
                case DB -> {
                    // Use batch resolution for database queries
                    Map<String, String> dbResults = databaseResolver.batchResolve(entries, tenantId, businessId);
                    for (Map.Entry<String, String> dbResult : dbResults.entrySet()) {
                        String argumentKey = dbResult.getKey();
                        String resolvedValue = dbResult.getValue();
                        MasterListEntry entry = entries.get(argumentKey);

                        if (resolvedValue != null) {
                            results.put(argumentKey, ResolvedArgument.success(
                                argumentKey, entry.toString(), dataSource, resolvedValue,
                                String.format("DB query: collection=%s, path=%s", entry.getCollection(), entry.getPath()),
                                System.currentTimeMillis() - sourceStartTime
                            ));
                        } else {
                            results.put(argumentKey, ResolvedArgument.failure(
                                argumentKey, entry.toString(), dataSource,
                                String.format("No value found in collection '%s' at path '%s'",
                                            entry.getCollection(), entry.getPath()),
                                System.currentTimeMillis() - sourceStartTime
                            ));
                        }
                    }
                }
                case GENERATE -> {
                    for (Map.Entry<String, MasterListEntry> entry : entries.entrySet()) {
                        results.put(entry.getKey(), resolveGeneratorArgument(entry.getKey(), entry.getValue(), request, tenantId, businessId));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error resolving arguments from {} data source: {}", dataSource, e.getMessage());

            // Mark all arguments from this source as failed
            for (String argumentKey : entries.keySet()) {
                results.put(argumentKey, ResolvedArgument.failure(
                    argumentKey, "unknown", dataSource, e.getMessage(),
                    System.currentTimeMillis() - sourceStartTime
                ));
            }
        }

        long sourceEndTime = System.currentTimeMillis();
        log.debug("Resolved {} arguments from {} data source in {}ms",
                 results.size(), dataSource, sourceEndTime - sourceStartTime);

        return results;
    }

    /**
     * Resolves a single payload argument.
     */
    private ResolvedArgument resolvePayloadArgument(String argumentKey, MasterListEntry entry,
                                                   TriggerEventRequestDto request, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();

        try {
            String value = payloadResolver.extractValue(request, entry.getPath(), headers);
            long endTime = System.currentTimeMillis();

            if (value != null) {
                return ResolvedArgument.success(argumentKey, entry.toString(), MasterListDataSource.PAYLOAD,
                    value, String.format("Payload path: %s", entry.getPath()), endTime - startTime);
            } else {
                return ResolvedArgument.failure(argumentKey, entry.toString(), MasterListDataSource.PAYLOAD,
                    String.format("No value found at payload path: %s", entry.getPath()), endTime - startTime);
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            return ResolvedArgument.failure(argumentKey, entry.toString(), MasterListDataSource.PAYLOAD,
                e.getMessage(), endTime - startTime);
        }
    }

    /**
     * Resolves a single token argument.
     */
    private ResolvedArgument resolveTokenArgument(String argumentKey, MasterListEntry entry,
                                                 TriggerEventRequestDto request) {
        long startTime = System.currentTimeMillis();

        try {
            String value = tokenResolver.extractValue(request, entry.getPath());
            long endTime = System.currentTimeMillis();

            if (value != null) {
                return ResolvedArgument.success(argumentKey, entry.toString(), MasterListDataSource.TOKEN,
                    value, String.format("JWT claim path: %s", entry.getPath()), endTime - startTime);
            } else {
                return ResolvedArgument.failure(argumentKey, entry.toString(), MasterListDataSource.TOKEN,
                    String.format("No value found at JWT path: %s", entry.getPath()), endTime - startTime);
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            return ResolvedArgument.failure(argumentKey, entry.toString(), MasterListDataSource.TOKEN,
                e.getMessage(), endTime - startTime);
        }
    }

    /**
     * Resolves a single generator argument.
     */
    private ResolvedArgument resolveGeneratorArgument(String argumentKey, MasterListEntry entry,
                                                     TriggerEventRequestDto request, String tenantId, String businessId) {
        long startTime = System.currentTimeMillis();

        try {
            String value = valueGenerator.generateValue(entry, request, tenantId, businessId);
            long endTime = System.currentTimeMillis();

            return ResolvedArgument.success(argumentKey, entry.toString(), MasterListDataSource.GENERATE,
                value, String.format("Generator: %s", entry.getGenerator()), endTime - startTime);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            return ResolvedArgument.failure(argumentKey, entry.toString(), MasterListDataSource.GENERATE,
                e.getMessage(), endTime - startTime);
        }
    }

    /**
     * Validates that all argument resolutions succeeded or applies default values if configured.
     *
     * @param resolutionResults the resolution results
     * @param argumentsMap original arguments map with master labels
     * @param config the master list configuration
     * @throws MasterListResolutionException if any resolution failed and default values are not enabled
     */
    private void validateAllResolutionsSucceeded(Map<String, ResolvedArgument> resolutionResults,
                                               Map<String, String> argumentsMap,
                                               MasterListConfig config) {
        List<ResolvedArgument> failedResolutions = resolutionResults.values().stream()
            .filter(result -> !result.isSuccess())
            .collect(Collectors.toList());

        if (failedResolutions.isEmpty()) {
            return; // All resolutions succeeded
        }

        // Check if default values should be used
        boolean useDefaultValues = masterListProperties.getResolution().isUseDefaultValues();

        if (useDefaultValues) {
            log.warn("Using default values for {}/{} failed argument resolutions",
                    failedResolutions.size(), resolutionResults.size());

            // Apply default values for failed resolutions
            for (ResolvedArgument failed : failedResolutions) {
                String argumentKey = failed.getArgumentKey();
                String masterLabel = argumentsMap.get(argumentKey);
                MasterListEntry entry = config.getEntry(masterLabel);

                if (entry != null) {
                    String defaultValue = entry.getDefaultValue();
                    log.info("Using default value '{}' for failed argument '{}' (master label: '{}')",
                            defaultValue, argumentKey, masterLabel);

                    // Replace failed result with successful result using default value
                    ResolvedArgument defaultResult = ResolvedArgument.success(
                        argumentKey,
                        masterLabel,
                        entry.getDataSource(),
                        defaultValue,
                        String.format("Default value used due to resolution failure: %s", failed.getErrorMessage()),
                        failed.getResolutionTimeMs()
                    );

                    resolutionResults.put(argumentKey, defaultResult);
                } else {
                    log.warn("No master list entry found for failed argument '{}' with label '{}'",
                            argumentKey, masterLabel);
                }
            }

            log.info("Successfully applied default values for failed argument resolutions");
            return;
        }

        // Default values not enabled - fail as before
        log.error("Failed to resolve {}/{} arguments (default values disabled):",
                 failedResolutions.size(), resolutionResults.size());

        failedResolutions.forEach(failed ->
            log.error("  - {}: {}", failed.getArgumentKey(), failed.getErrorMessage()));

        MasterListResolutionException exception = new MasterListResolutionException(
            String.format("Failed to resolve %d/%d template arguments",
                         failedResolutions.size(), resolutionResults.size()));

        exception.setFailedResolutions(failedResolutions);
        throw exception;
    }

    /**
     * Replaces placeholders in template text with resolved values.
     *
     * @param templateText the template text with placeholders
     * @param resolvedValues map of placeholders to resolved values
     * @return the template text with placeholders replaced
     */
    private String replacePlaceholders(String templateText, Map<String, String> resolvedValues) {
        if (templateText == null || templateText.trim().isEmpty()) {
            return templateText;
        }

        if (resolvedValues == null || resolvedValues.isEmpty()) {
            return templateText;
        }

        String result = templateText;
        int replacementCount = 0;

        for (Map.Entry<String, String> entry : resolvedValues.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";

            if (result.contains(placeholder)) {
                result = result.replace(placeholder, value);
                replacementCount++;
            }
        }

        log.debug("Replaced {} placeholders in template text", replacementCount);
        return result;
    }

    /**
     * Custom exception for master list resolution errors.
     */
    public static class MasterListResolutionException extends RuntimeException {
        private List<ResolvedArgument> failedResolutions;

        public MasterListResolutionException(String message) {
            super(message);
        }

        public MasterListResolutionException(String message, Throwable cause) {
            super(message, cause);
        }

        public List<ResolvedArgument> getFailedResolutions() {
            return failedResolutions;
        }

        public void setFailedResolutions(List<ResolvedArgument> failedResolutions) {
            this.failedResolutions = failedResolutions;
        }
    }
}