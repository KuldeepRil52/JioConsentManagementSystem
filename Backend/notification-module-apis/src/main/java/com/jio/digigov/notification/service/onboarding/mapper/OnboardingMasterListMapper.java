package com.jio.digigov.notification.service.onboarding.mapper;

import com.jio.digigov.notification.dto.masterlist.MasterListEntry;
import com.jio.digigov.notification.dto.onboarding.MasterLabelDefinition;
import com.jio.digigov.notification.dto.request.masterlist.CreateMasterListRequestDto;
import com.jio.digigov.notification.enums.MasterListDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper to convert MasterLabelDefinition list to CreateMasterListRequestDto.
 *
 * This mapper transforms the master label definitions from the onboarding provider
 * into the request DTO required by TenantMasterListConfigService.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Component
@Slf4j
public class OnboardingMasterListMapper {

    /**
     * Converts a list of MasterLabelDefinitions to CreateMasterListRequestDto.
     *
     * @param definitions List of master label definitions from the provider
     * @param description Optional description for the master list configuration
     * @return CreateMasterListRequestDto for master list creation
     */
    public CreateMasterListRequestDto toRequest(
            List<MasterLabelDefinition> definitions,
            String description) {

        // Convert list to map: labelName -> MasterListEntry
        Map<String, MasterListEntry> masterListConfig = definitions.stream()
                .collect(Collectors.toMap(
                        MasterLabelDefinition::getLabelName,
                        this::toMasterListEntry,
                        (existing, replacement) -> {
                            log.warn("Duplicate label found, keeping first: {}", existing);
                            return existing;
                        }
                ));

        return CreateMasterListRequestDto.builder()
                .masterListConfig(masterListConfig)
                .eventMappings(null)  // No event mappings for onboarding
                .description(description != null ? description : "Default master list configuration created during onboarding")
                .build();
    }

    /**
     * Converts a single MasterLabelDefinition to MasterListEntry.
     *
     * @param definition The master label definition
     * @return MasterListEntry
     */
    private MasterListEntry toMasterListEntry(MasterLabelDefinition definition) {
        // Convert String dataSource to enum
        MasterListDataSource dataSourceEnum = null;
        if (definition.getDataSource() != null) {
            try {
                dataSourceEnum = MasterListDataSource.valueOf(definition.getDataSource().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid dataSource value: {}, defaulting to PAYLOAD", definition.getDataSource());
                dataSourceEnum = MasterListDataSource.PAYLOAD;
            }
        }

        // Convert Map<String, String> config to Map<String, Object>
        Map<String, Object> configObj = null;
        if (definition.getConfig() != null) {
            configObj = new HashMap<>(definition.getConfig());
        }

        return MasterListEntry.builder()
                .dataSource(dataSourceEnum)
                .collection(definition.getCollection())
                .path(definition.getPath())
                .generator(definition.getGenerator())
                .query(definition.getQuery())
                .config(configObj)
                .defaultValue(definition.getDefaultValue())
                .build();
    }
}
