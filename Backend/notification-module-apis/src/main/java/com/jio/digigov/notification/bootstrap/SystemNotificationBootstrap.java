package com.jio.digigov.notification.bootstrap;

import com.jio.digigov.notification.service.system.SystemOnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bootstrap component for system-wide notification auto-onboarding.
 *
 * Runs on application startup and checks if system notification configuration
 * needs to be onboarded. If configuration doesn't exist in shared database,
 * it automatically loads from static JSON files and saves to database.
 *
 * Execution Order:
 * - Runs with LOWEST_PRECEDENCE to ensure all beans are initialized
 * - Executes after OnboardingDataLoader (which loads JSON into memory)
 *
 * @author Notification Service Team
 * @since 2025-01-21
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class SystemNotificationBootstrap implements ApplicationRunner {

    private final SystemOnboardingService systemOnboardingService;

    @Value("${system.notification.enabled:false}")
    private boolean systemNotificationEnabled;

    @Value("${system.notification.auto-onboard:true}")
    private boolean autoOnboard;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!systemNotificationEnabled) {
            log.debug("System notification is disabled, skipping bootstrap");
            return;
        }

        log.info("========================================");
        log.info("System Notification Bootstrap Starting");
        log.info("========================================");

        try {
            // Check what needs to be onboarded
            var onboardingStatus = systemOnboardingService.checkOnboardingStatus();

            // Log detailed status
            log.info("Onboarding Status Check:");
            log.info("  - Config needed: {}", onboardingStatus.isConfigNeeded());
            log.info("  - Master labels needed: {}", onboardingStatus.isMasterLabelsNeeded());
            log.info("  - Missing templates: {}", onboardingStatus.getMissingTemplateEvents().size());
            log.info("  - Missing event configs: {}", onboardingStatus.getMissingEventConfigs().size());

            if (onboardingStatus.isAnyOnboardingNeeded()) {
                if (autoOnboard) {
                    log.info("Starting auto-onboarding for missing components...");

                    systemOnboardingService.autoOnboardSystemNotifications(onboardingStatus);

                    log.info("✓ System notification auto-onboarding completed successfully!");
                } else {
                    log.warn("Missing system notification components detected, but auto-onboard is disabled");
                    log.warn("Please run onboarding manually or set system.notification.auto-onboard=true");
                    if (!onboardingStatus.getMissingTemplateEvents().isEmpty()) {
                        log.warn("Missing template events: {}", onboardingStatus.getMissingTemplateEvents());
                    }
                    if (!onboardingStatus.getMissingEventConfigs().isEmpty()) {
                        log.warn("Missing event configs: {}", onboardingStatus.getMissingEventConfigs());
                    }
                }
            } else {
                log.info("✓ All system notification components are up-to-date in shared database");
            }

        } catch (Exception e) {
            log.error("Failed to bootstrap system notifications: {}", e.getMessage());
            log.warn("Application will continue, but system notifications may not work properly");
            // Don't fail application startup - just log the error
        }

        log.info("========================================");
        log.info("System Notification Bootstrap Complete");
        log.info("========================================");
    }
}
