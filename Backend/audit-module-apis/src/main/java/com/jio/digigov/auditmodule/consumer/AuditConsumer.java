package com.jio.digigov.auditmodule.consumer;

import com.jio.digigov.auditmodule.config.MultiTenantMongoConfig;
import com.jio.digigov.auditmodule.entity.AuditDocument;
import com.jio.digigov.auditmodule.util.AuditChainUtil;
import com.jio.digigov.auditmodule.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditConsumer {

    private final MultiTenantMongoConfig mongoConfig;
    private static final Object dataHashLock = new Object();

    @KafkaListener(topics = "${kafka.topics.audit}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(AuditDocument doc, Acknowledgment acknowledgment) {
        log.info("Consumed audit event | auditId={} tenantId={} businessId={}",
                doc.getAuditId(), doc.getTenantId(), doc.getBusinessId());

        try {
            TenantContextHolder.setTenant(doc.getTenantId());
            TenantContextHolder.setBusinessId(doc.getBusinessId());

            MongoTemplate tenantMongoTemplate =
                    mongoConfig.getMongoTemplateForTenant(doc.getTenantId());

            synchronized (dataHashLock) {
                // Fetch latest chain
                String prevChain = AuditChainUtil.fetchLatestChain(tenantMongoTemplate, AuditDocument.class);

                // Recompute currentChainHash (safe chain linking)
                String recalculatedChain = AuditChainUtil.recomputeChain(prevChain, doc.getPayloadHash());
                doc.setCurrentChainHash(recalculatedChain);

                // Save to DB
                tenantMongoTemplate.save(doc);
                log.info("Audit saved successfully | auditId={} currentChainHash={}",
                        doc.getAuditId(), doc.getCurrentChainHash());
            }

            acknowledgment.acknowledge();
            log.info("Acknowledged Kafka message | auditId={}", doc.getAuditId());
        } catch (Exception e) {
            log.error("Error saving audit | auditId={} error={}", doc.getAuditId(), e.getMessage(), e);
        } finally {
            TenantContextHolder.clear();
        }
    }
}