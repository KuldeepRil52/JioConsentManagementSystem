package com.jio.digigov.notification.config.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Kafka Connection Validator for the DPDP Notification Consumer.
 *
 * This component validates Kafka connectivity at application startup by attempting
 * to connect to and verify the existence of all required Kafka topics. It provides
 * clear logging of connection status for monitoring and troubleshooting.
 *
 * Validation Strategy:
 * - Runs after application is fully started using ApplicationReadyEvent
 * - Uses AdminClient to list and describe topics
 * - Validates connectivity to all three notification topics:
 *   - DEV_CMS_NOTIFICATION_SMS
 *   - DEV_CMS_NOTIFICATION_EMAIL
 *   - DEV_CMS_NOTIFICATION_CALLBACK
 * - Logs SUCCESS/FAILURE for each topic
 * - Continues startup even if validation fails (non-blocking)
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaConnectionValidator {

    private final KafkaAdmin kafkaAdmin;

    @Value("${kafka.topics.sms}")
    private String smsTopicName;

    @Value("${kafka.topics.email}")
    private String emailTopicName;

    @Value("${kafka.topics.callback}")
    private String callbackTopicName;

    @Value("${kafka.consumer.group-id}")
    private String consumerGroupId;

    /**
     * Validates Kafka connectivity after application is fully started.
     * This method is called automatically after all beans are initialized
     * and Kafka consumers have started and been assigned partitions.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateKafkaConnection() {
        log.info("========================================");
        log.info("Starting Kafka Connection Validation");
        log.info("========================================");

        List<String> topicsToValidate = Arrays.asList(
            smsTopicName,
            emailTopicName,
            callbackTopicName
        );

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {

            // Test 1: List all topics to verify basic connectivity
            log.info("Test 1: Checking Kafka cluster connectivity...");
            try {
                Set<String> existingTopics = adminClient.listTopics()
                    .names()
                    .get(30, TimeUnit.SECONDS);

                log.info("✓ SUCCESS: Connected to Kafka cluster");
                log.info("Found {} topics in the cluster", existingTopics.size());
            } catch (Exception e) {
                log.error("✗ FAILURE: Unable to connect to Kafka cluster");
                log.error("Root cause: {}", e.getMessage());
                log.info("========================================");
                log.info("Kafka Connection Validation FAILED");
                log.info("========================================");
                return;
            }

            // Test 2: Validate each required topic and get partition information
            log.info("Test 2: Validating required notification topics...");
            int successCount = 0;
            int failureCount = 0;

            for (String topicName : topicsToValidate) {
                try {
                    DescribeTopicsResult describeResult = adminClient.describeTopics(
                        Arrays.asList(topicName)
                    );

                    Map<String, TopicDescription> topicDescriptions = describeResult.allTopicNames()
                        .get(30, TimeUnit.SECONDS);

                    TopicDescription topicDesc = topicDescriptions.get(topicName);
                    int partitionCount = topicDesc.partitions().size();
                    String partitionIds = topicDesc.partitions().stream()
                        .map(p -> String.valueOf(p.partition()))
                        .collect(Collectors.joining(", "));

                    log.info("✓ SUCCESS: Topic '{}' is accessible", topicName);
                    log.info("  Partitions: {} [{}]", partitionCount, partitionIds);
                    successCount++;

                } catch (Exception e) {
                    log.error("✗ FAILURE: Topic '{}' is NOT accessible", topicName);
                    log.error("  Reason: {}", e.getMessage());
                    failureCount++;
                }
            }

            // Test 3: Check consumer group assignment
            log.info("Test 3: Checking consumer group assignments...");
            try {
                validateConsumerGroupAssignments(adminClient);
            } catch (Exception e) {
                log.warn("⚠ Unable to retrieve consumer group information: {}", e.getMessage());
                log.debug("Consumer group validation error details:", e);
            }

            // Summary
            log.info("========================================");
            log.info("Kafka Connection Validation Summary:");
            log.info("  Total topics checked: {}", topicsToValidate.size());
            log.info("  Successful: {}", successCount);
            log.info("  Failed: {}", failureCount);

            if (failureCount == 0) {
                log.info("✓ All Kafka topics are accessible");
                log.info("Kafka Connection Validation PASSED");
            } else {
                log.warn("⚠ Some Kafka topics are not accessible");
                log.warn("Kafka Connection Validation COMPLETED WITH WARNINGS");
            }
            log.info("========================================");

        } catch (Exception e) {
            log.error("========================================");
            log.error("✗ CRITICAL ERROR during Kafka validation");
            log.error("Error: {}", e.getMessage());
            log.error("Kafka Connection Validation FAILED");
            log.error("========================================");
        }
    }

    /**
     * Validates consumer group assignments and logs partition details.
     */
    private void validateConsumerGroupAssignments(AdminClient adminClient) {
        try {
            // Describe the consumer group
            Map<String, ConsumerGroupDescription> groupDescriptions = adminClient
                .describeConsumerGroups(Arrays.asList(consumerGroupId))
                .all()
                .get(30, TimeUnit.SECONDS);

            ConsumerGroupDescription groupDesc = groupDescriptions.get(consumerGroupId);

            if (groupDesc == null) {
                log.info("Consumer Group '{}': No active consumers (group may not be initialized yet)",
                         consumerGroupId);
                return;
            }

            Collection<MemberDescription> members = groupDesc.members();

            if (members.isEmpty()) {
                log.info("Consumer Group '{}': No active consumers currently connected", consumerGroupId);
                log.info("  State: {}", groupDesc.state());
                return;
            }

            log.info("Consumer Group '{}': {} active consumer(s)", consumerGroupId, members.size());
            log.info("  State: {}", groupDesc.state());

            int memberIndex = 1;
            for (MemberDescription member : members) {
                log.info("  Consumer #{}: clientId={}, host={}",
                         memberIndex++,
                         member.clientId(),
                         member.host());

                Set<TopicPartition> assignedPartitions = member.assignment().topicPartitions();

                if (assignedPartitions.isEmpty()) {
                    log.info("    Assigned Partitions: None (waiting for rebalance)");
                } else {
                    // Group partitions by topic
                    Map<String, List<TopicPartition>> partitionsByTopic = assignedPartitions.stream()
                        .collect(Collectors.groupingBy(TopicPartition::topic));

                    partitionsByTopic.forEach((topic, partitions) -> {
                        String partitionList = partitions.stream()
                            .map(tp -> String.valueOf(tp.partition()))
                            .collect(Collectors.joining(", "));
                        log.info("    Topic '{}': listening to partition(s) [{}]",
                                 topic, partitionList);
                    });
                }
            }

        } catch (org.apache.kafka.common.errors.GroupIdNotFoundException e) {
            log.info("Consumer Group '{}': Not found (consumers may not have started yet)", consumerGroupId);
        } catch (Exception e) {
            log.warn("Unable to describe consumer group '{}': {}", consumerGroupId, e.getMessage());
            log.debug("Consumer group description error:", e);
        }
    }
}
