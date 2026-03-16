package com.jio.digigov.notification.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Logs Kafka partition assignments in real-time as they happen during consumer group rebalancing.
 *
 * This listener provides visibility into:
 * - When partitions are assigned to this consumer
 * - When partitions are revoked from this consumer
 * - Which topics and partitions are involved
 *
 * Use this to monitor consumer rebalancing behavior and troubleshoot partition assignment issues.
 *
 * @author Notification Service Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class PartitionAssignmentLogger implements ConsumerRebalanceListener {

    /**
     * Called before partitions are revoked from this consumer.
     * This happens at the start of a rebalance.
     */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        if (partitions.isEmpty()) {
            return;
        }

        log.info("========================================");
        log.info("⚠ KAFKA REBALANCE: Partitions REVOKED");
        log.info("========================================");

        Map<String, String> partitionsByTopic = partitions.stream()
            .collect(Collectors.groupingBy(
                TopicPartition::topic,
                Collectors.mapping(
                    tp -> String.valueOf(tp.partition()),
                    Collectors.joining(", ")
                )
            ));

        partitionsByTopic.forEach((topic, partitionList) ->
            log.info("  Topic '{}': revoked partition(s) [{}]", topic, partitionList)
        );

        log.info("========================================");
    }

    /**
     * Called after partitions are assigned to this consumer.
     * This happens at the end of a rebalance.
     */
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (partitions.isEmpty()) {
            log.info("========================================");
            log.info("⚠ KAFKA REBALANCE: No partitions assigned to this consumer");
            log.info("  Reason: More consumers than partitions (consumer is idle/standby)");
            log.info("========================================");
            return;
        }

        log.info("========================================");
        log.info("✓ KAFKA REBALANCE: Partitions ASSIGNED");
        log.info("========================================");

        Map<String, String> partitionsByTopic = partitions.stream()
            .collect(Collectors.groupingBy(
                TopicPartition::topic,
                Collectors.mapping(
                    tp -> String.valueOf(tp.partition()),
                    Collectors.joining(", ")
                )
            ));

        partitionsByTopic.forEach((topic, partitionList) ->
            log.info("  Topic '{}': listening to partition(s) [{}]", topic, partitionList)
        );

        log.info("  Total partitions assigned: {}", partitions.size());
        log.info("========================================");
    }

    /**
     * Called when partitions are lost (ungraceful shutdown scenario).
     * Defaults to revoking the partitions.
     */
    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        if (partitions.isEmpty()) {
            return;
        }

        log.warn("========================================");
        log.warn("✗ KAFKA REBALANCE: Partitions LOST (ungraceful)");
        log.warn("========================================");

        Map<String, String> partitionsByTopic = partitions.stream()
            .collect(Collectors.groupingBy(
                TopicPartition::topic,
                Collectors.mapping(
                    tp -> String.valueOf(tp.partition()),
                    Collectors.joining(", ")
                )
            ));

        partitionsByTopic.forEach((topic, partitionList) ->
            log.warn("  Topic '{}': lost partition(s) [{}]", topic, partitionList)
        );

        log.warn("========================================");

        // Call the default behavior
        ConsumerRebalanceListener.super.onPartitionsLost(partitions);
    }
}
