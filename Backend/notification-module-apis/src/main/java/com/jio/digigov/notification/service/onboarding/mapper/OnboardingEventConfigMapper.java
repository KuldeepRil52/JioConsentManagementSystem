package com.jio.digigov.notification.service.onboarding.mapper;

import com.jio.digigov.notification.dto.onboarding.EventConfigDefinition;
import com.jio.digigov.notification.dto.request.event.CreateEventConfigurationRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper to convert EventConfigDefinition to CreateEventConfigurationRequestDto.
 *
 * This mapper transforms the event config definitions from the onboarding provider
 * into the request DTOs required by EventConfigurationService.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Component
@Slf4j
public class OnboardingEventConfigMapper {

    /**
     * Converts an EventConfigDefinition to a CreateEventConfigurationRequestDto.
     *
     * @param definition The event config definition from the provider
     * @return CreateEventConfigurationRequestDto for event config creation
     */
    public CreateEventConfigurationRequestDto toRequest(EventConfigDefinition definition) {
        CreateEventConfigurationRequestDto request = new CreateEventConfigurationRequestDto();
        request.setEventType(definition.getEventType());
        request.setDescription(definition.getDescription());
        request.setPriority(definition.getPriority() != null ? definition.getPriority().name() : "MEDIUM");
        request.setIsActive(true);  // Always active for onboarding

        // Build notifications config
        CreateEventConfigurationRequestDto.NotificationsConfigDto notifications =
                new CreateEventConfigurationRequestDto.NotificationsConfigDto();

        // Data Principal config (channels: SMS, EMAIL)
        if (definition.isNotifyDataPrincipal()) {
            CreateEventConfigurationRequestDto.RecipientConfigDto dpConfig =
                    new CreateEventConfigurationRequestDto.RecipientConfigDto();
            dpConfig.setEnabled(true);
            dpConfig.setChannels(java.util.Arrays.asList("SMS", "EMAIL"));  // Default channels
            notifications.setDataPrincipal(dpConfig);
        }

        // Data Fiduciary config (method: CALLBACK)
        if (definition.isNotifyDataFiduciary()) {
            CreateEventConfigurationRequestDto.RecipientConfigDto dfConfig =
                    new CreateEventConfigurationRequestDto.RecipientConfigDto();
            dfConfig.setEnabled(true);
            dfConfig.setMethod("CALLBACK");
            notifications.setDataFiduciary(dfConfig);
        }

        // Data Processor config (method: CALLBACK)
        if (definition.isNotifyDataProcessor()) {
            CreateEventConfigurationRequestDto.RecipientConfigDto dprocConfig =
                    new CreateEventConfigurationRequestDto.RecipientConfigDto();
            dprocConfig.setEnabled(true);
            dprocConfig.setMethod("CALLBACK");
            notifications.setDataProcessor(dprocConfig);
        }

        // Data Protection Officer config (channels: EMAIL only for grievance events)
        if (definition.isNotifyDpo()) {
            CreateEventConfigurationRequestDto.RecipientConfigDto dpoConfig =
                    new CreateEventConfigurationRequestDto.RecipientConfigDto();
            dpoConfig.setEnabled(true);
            dpoConfig.setChannels(java.util.Arrays.asList("EMAIL"));  // DPO receives EMAIL only
            notifications.setDataProtectionOfficer(dpoConfig);
        }

        request.setNotifications(notifications);

        return request;
    }
}
