package com.jio.digigov.notification.dto.response.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Results summary for an onboarding job.
 *
 * Contains counts of created/failed items for each onboarding component.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResultsDto {

    /**
     * Master list labels results
     */
    private MasterListResultDto masterListLabels;

    /**
     * Templates results
     */
    private TemplateResultDto templates;

    /**
     * Event configurations results
     */
    private EventConfigResultDto eventConfigurations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasterListResultDto {
        private int created;
        private int failed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateResultDto {
        private int created;
        private int failed;
        private int smsCount;
        private int emailCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventConfigResultDto {
        private int created;
        private int failed;
    }
}
