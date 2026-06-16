package com.jio.digigov.notification.service.onboarding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.digigov.notification.dto.onboarding.json.EventConfigDataFile;
import com.jio.digigov.notification.dto.onboarding.json.MasterLabelDataFile;
import com.jio.digigov.notification.dto.onboarding.json.TemplateDataFile;
import com.jio.digigov.notification.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to load onboarding data from JSON files.
 *
 * <p>Loads templates, event configs, and master labels from separate JSON files.</p>
 *
 * @author DPDP Notification Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingDataLoader {

    private final ObjectMapper objectMapper;

    private List<TemplateDataFile> templates;
    private List<EventConfigDataFile> eventConfigs;
    private List<MasterLabelDataFile> masterLabels;

    /**
     * Loads all onboarding data on application startup.
     */
    @PostConstruct
    public void loadOnboardingData() {
        log.info("Loading onboarding data from JSON files...");

        loadTemplates();
        loadEventConfigs();
        loadMasterLabels();

        log.info("Successfully loaded onboarding data: {} templates, {} event configs, {} master labels",
                templates.size(), eventConfigs.size(), masterLabels.size());
    }

    /**
     * Loads template data from templates.json.
     */
    private void loadTemplates() {
        try {
            ClassPathResource resource = new ClassPathResource("system-notifications/templates.json");

            if (!resource.exists()) {
                log.warn("templates.json not found");
                templates = new ArrayList<>();
                return;
            }

            templates = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<TemplateDataFile>>() {}
            );

            log.info("Loaded {} template configurations", templates.size());

        } catch (IOException e) {
            log.error("Failed to load templates.json");
            throw new BusinessException("TEMPLATE_LOAD_ERROR",
                    "Failed to load template data: " + e.getMessage());
        }
    }

    /**
     * Loads event config data from event-configs.json.
     */
    private void loadEventConfigs() {
        try {
            ClassPathResource resource = new ClassPathResource("system-notifications/event-configs.json");

            if (!resource.exists()) {
                log.warn("event-configs.json not found");
                eventConfigs = new ArrayList<>();
                return;
            }

            eventConfigs = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<EventConfigDataFile>>() {}
            );

            log.info("Loaded {} event configurations", eventConfigs.size());

        } catch (IOException e) {
            log.error("Failed to load event-configs.json");
            throw new BusinessException("EVENT_CONFIG_LOAD_ERROR",
                    "Failed to load event config data: " + e.getMessage());
        }
    }

    /**
     * Loads master label data from master-labels.json.
     */
    private void loadMasterLabels() {
        try {
            ClassPathResource resource = new ClassPathResource("system-notifications/master-labels.json");

            if (!resource.exists()) {
                log.warn("master-labels.json not found");
                masterLabels = new ArrayList<>();
                return;
            }

            masterLabels = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<MasterLabelDataFile>>() {}
            );

            log.info("Loaded {} master labels", masterLabels.size());

        } catch (IOException e) {
            log.error("Failed to load master-labels.json");
            throw new BusinessException("MASTER_LABEL_LOAD_ERROR",
                    "Failed to load master label data: " + e.getMessage());
        }
    }

    /**
     * Gets all loaded templates.
     */
    public List<TemplateDataFile> getAllTemplates() {
        return new ArrayList<>(templates);
    }

    /**
     * Gets all loaded event configs.
     */
    public List<EventConfigDataFile> getAllEventConfigs() {
        return new ArrayList<>(eventConfigs);
    }

    /**
     * Gets all loaded master labels.
     */
    public List<MasterLabelDataFile> getAllMasterLabels() {
        return new ArrayList<>(masterLabels);
    }

    /**
     * Checks if data is loaded.
     */
    public boolean isDataLoaded() {
        return templates != null && eventConfigs != null && masterLabels != null;
    }

    /**
     * Reloads all data.
     */
    public void reload() {
        log.info("Reloading onboarding data...");
        loadOnboardingData();
    }
}
