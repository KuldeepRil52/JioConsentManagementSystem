package com.jio.digigov.notification.dto.response.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for notification count responses with breakdown by type and status.
 *
 * Provides comprehensive statistics for notification events and associated
 * notifications across all types (SMS, Email, Callback).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountResponseDto {

    /**
     * Total count of notification events
     */
    private Long totalEvents;

    /**
     * Total count of all notifications across all types
     */
    private Long totalNotifications;

    /**
     * Count breakdown by event status
     */
    private EventStatusCounts eventStatusCounts;

    /**
     * Count breakdown by notification type
     */
    private NotificationTypeCounts notificationTypeCounts;

    /**
     * Count breakdown by notification status across all types
     */
    private NotificationStatusCounts notificationStatusCounts;

    /**
     * Count breakdown by event type
     */
    private Map<String, Long> eventTypeCounts;

    /**
     * Count breakdown by priority
     */
    private Map<String, Long> priorityCounts;

    /**
     * Count breakdown by resource type
     */
    private Map<String, Long> resourceCounts;

    /**
     * Count breakdown by source system
     */
    private Map<String, Long> sourceCounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventStatusCounts {
        private Long pending;
        private Long processing;
        private Long completed;
        private Long failed;
        private Long cancelled;

        @Builder.Default
        private Long total = 0L;

        public void calculateTotal() {
            this.total = (pending != null ? pending : 0L) +
                        (processing != null ? processing : 0L) +
                        (completed != null ? completed : 0L) +
                        (failed != null ? failed : 0L) +
                        (cancelled != null ? cancelled : 0L);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationTypeCounts {
        private Long sms;
        private Long email;
        private Long callback;

        @Builder.Default
        private Long total = 0L;

        public void calculateTotal() {
            this.total = (sms != null ? sms : 0L) +
                        (email != null ? email : 0L) +
                        (callback != null ? callback : 0L);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationStatusCounts {
        private Long pending;
        private Long processing;
        private Long sent;
        private Long delivered;
        private Long acknowledged;
        private Long failed;
        private Long retryScheduled;
        private Long retrieved;

        @Builder.Default
        private Long total = 0L;

        public void calculateTotal() {
            this.total = (pending != null ? pending : 0L) +
                        (processing != null ? processing : 0L) +
                        (sent != null ? sent : 0L) +
                        (delivered != null ? delivered : 0L) +
                        (acknowledged != null ? acknowledged : 0L) +
                        (failed != null ? failed : 0L) +
                        (retryScheduled != null ? retryScheduled : 0L) +
                        (retrieved != null ? retrieved : 0L);
        }
    }

    /**
     * SMS-specific count breakdown
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsStatusCounts {
        private Long pending;
        private Long processing;
        private Long sent;
        private Long delivered;
        private Long failed;
        private Long retryScheduled;

        @Builder.Default
        private Long total = 0L;

        public void calculateTotal() {
            this.total = (pending != null ? pending : 0L) +
                        (processing != null ? processing : 0L) +
                        (sent != null ? sent : 0L) +
                        (delivered != null ? delivered : 0L) +
                        (failed != null ? failed : 0L) +
                        (retryScheduled != null ? retryScheduled : 0L);
        }
    }

    /**
     * Email-specific count breakdown
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailStatusCounts {
        private Long pending;
        private Long processing;
        private Long sent;
        private Long delivered;
        private Long opened;
        private Long failed;
        private Long retryScheduled;

        @Builder.Default
        private Long total = 0L;

        public void calculateTotal() {
            this.total = (pending != null ? pending : 0L) +
                        (processing != null ? processing : 0L) +
                        (sent != null ? sent : 0L) +
                        (delivered != null ? delivered : 0L) +
                        (opened != null ? opened : 0L) +
                        (failed != null ? failed : 0L) +
                        (retryScheduled != null ? retryScheduled : 0L);
        }
    }

    /**
     * Callback-specific count breakdown
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackStatusCounts {
        private Long pending;
        private Long processing;
        private Long sent;
        private Long acknowledged;
        private Long failed;
        private Long retryScheduled;
        private Long retrieved;

        @Builder.Default
        private Long total = 0L;

        public void calculateTotal() {
            this.total = (pending != null ? pending : 0L) +
                        (processing != null ? processing : 0L) +
                        (sent != null ? sent : 0L) +
                        (acknowledged != null ? acknowledged : 0L) +
                        (failed != null ? failed : 0L) +
                        (retryScheduled != null ? retryScheduled : 0L) +
                        (retrieved != null ? retrieved : 0L);
        }
    }

    /**
     * Detailed counts by notification type and status
     */
    private SmsStatusCounts smsStatusCounts;
    private EmailStatusCounts emailStatusCounts;
    private CallbackStatusCounts callbackStatusCounts;

    /**
     * Calculate all totals after setting individual counts
     */
    public void calculateTotals() {
        if (eventStatusCounts != null) {
            eventStatusCounts.calculateTotal();
        }

        if (notificationTypeCounts != null) {
            notificationTypeCounts.calculateTotal();
            this.totalNotifications = notificationTypeCounts.getTotal();
        }

        if (notificationStatusCounts != null) {
            notificationStatusCounts.calculateTotal();
        }

        if (smsStatusCounts != null) {
            smsStatusCounts.calculateTotal();
        }

        if (emailStatusCounts != null) {
            emailStatusCounts.calculateTotal();
        }

        if (callbackStatusCounts != null) {
            callbackStatusCounts.calculateTotal();
        }
    }
}