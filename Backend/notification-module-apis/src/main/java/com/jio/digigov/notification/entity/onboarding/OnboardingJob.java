package com.jio.digigov.notification.entity.onboarding;

import com.jio.digigov.notification.entity.BaseEntity;
import com.jio.digigov.notification.enums.OnboardingJobStatus;
import com.jio.digigov.notification.enums.OnboardingStep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Onboarding Job Entity
 *
 * Represents an asynchronous onboarding job that creates default notification infrastructure
 * for a business within a tenant's environment.
 *
 * IMPORTANT: This entity is stored in a tenant-specific database (tenant_db_{tenantId}).
 * Therefore, there is NO tenantId field in the document - tenant isolation is achieved
 * through database separation, not document-level fields.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2025-01-21
 */
@Document(collection = "onboarding_jobs")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "business_status_idx",
                  def = "{'businessId': 1, 'status': 1}"),
    @CompoundIndex(name = "business_created_idx",
                  def = "{'businessId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "job_id_idx",
                  def = "{'jobId': 1}", unique = true)
})
public class OnboardingJob extends BaseEntity {

    /**
     * Unique job identifier (e.g., ONB-20250121-103045-A1B2C3D4)
     */
    @Indexed(unique = true)
    @Field("jobId")
    @NotBlank(message = "Job ID is required")
    private String jobId;

    /**
     * Business identifier within the tenant
     * NOTE: NO tenantId field - tenant isolation via database name (tenant_db_{tenantId})
     */
    @Indexed
    @Field("businessId")
    @NotBlank(message = "Business ID is required")
    private String businessId;

    /**
     * Current status of the onboarding job
     */
    @Field("status")
    @NotNull(message = "Status is required")
    private OnboardingJobStatus status;

    /**
     * Current processing step
     */
    @Field("currentStep")
    private OnboardingStep currentStep;

    /**
     * Progress percentage (0-100)
     */
    @Field("progressPercentage")
    @Builder.Default
    private Integer progressPercentage = 0;

    /**
     * Transaction ID for tracking and correlation
     */
    @Field("transactionId")
    private String transactionId;

    /**
     * Request parameters that initiated this job
     */
    @Field("requestParams")
    private OnboardingRequestParams requestParams;

    /**
     * Results of the onboarding process
     */
    @Field("results")
    private OnboardingResults results;

    /**
     * List of error messages encountered during processing
     */
    @Field("errors")
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * Timestamp when job processing started
     */
    @Field("startedAt")
    private LocalDateTime startedAt;

    /**
     * Timestamp when job processing completed
     */
    @Field("completedAt")
    private LocalDateTime completedAt;

    /**
     * Embedded class for request parameters
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingRequestParams {

        @Field("createTemplates")
        private boolean createTemplates;

        @Field("createEventConfigurations")
        private boolean createEventConfigurations;

        @Field("createMasterList")
        private boolean createMasterList;

        @Field("description")
        private String description;
    }

    /**
     * Embedded class for onboarding results
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingResults {

        @Field("masterListLabels")
        private MasterListResult masterListLabels;

        @Field("templates")
        private TemplateResult templates;

        @Field("eventConfigurations")
        private EventConfigResult eventConfigurations;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MasterListResult {
            @Field("created")
            private int created;

            @Field("failed")
            private int failed;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TemplateResult {
            @Field("created")
            private int created;

            @Field("failed")
            private int failed;

            @Field("smsCount")
            private int smsCount;

            @Field("emailCount")
            private int emailCount;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class EventConfigResult {
            @Field("created")
            private int created;

            @Field("failed")
            private int failed;
        }
    }
}
