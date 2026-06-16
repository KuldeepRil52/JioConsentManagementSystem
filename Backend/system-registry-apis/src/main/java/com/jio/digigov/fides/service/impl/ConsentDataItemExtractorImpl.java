package com.jio.digigov.fides.service.impl;

import com.jio.digigov.fides.dto.ConsentWithdrawalDataItems;
import com.jio.digigov.fides.entity.Consent;
import com.jio.digigov.fides.config.MultiTenantMongoConfig;
import com.jio.digigov.fides.service.UnCommonDataExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentDataItemExtractorImpl implements UnCommonDataExtractor {

    private final MultiTenantMongoConfig mongoConfig;

    @Override
    public ConsentWithdrawalDataItems extractWithdrawalDataItems(Consent consent, String tenantId, String businessId) {
        try {
            Set<String> withdrawnItems = withdrawnConsentDataItems(consent);
            Map<String, Set<String>> activeItems = getActiveConsents(consent, tenantId, businessId);

            // Items eligible for deletion
            Set<String> deletable =
                    withdrawnItems.stream()
                            .filter(item -> !activeItems.containsKey(item))
                            .collect(Collectors.toSet());

            // Items that MUST be deferred
            Map<String, Set<String>> deferred =
                    withdrawnItems.stream()
                            .filter(activeItems::containsKey)
                            .collect(Collectors.toMap(
                                item -> item,
                                activeItems::get
                        ));

            log.info("Deletable PII with TRAI: {}", deletable);
            log.info("Deferred PII (active consent): {}", deferred);

            return new ConsentWithdrawalDataItems(deletable, deferred);
        } catch (Exception e) {
            log.error("Error while extracting withdrawal data items. consentId={}, tenantId={}, businessId={}",
                    consent != null ? consent.getConsentId() : null,
                    tenantId,
                    businessId,
                    e);
            return new ConsentWithdrawalDataItems(Set.of(), Map.of());
        }
    }
    /*
        gets the set of all the dataitems that the user is withdawing the consent for
     */

    public Set<String> withdrawnConsentDataItems(Consent consent) {
        try {
            if (consent == null || consent.getPreferences() == null) {
                return Set.of();
            }

            Set<String> dataItems = consent.getPreferences().stream()
                    .filter(p -> p.getProcessorActivityList() != null)
                    .flatMap(p -> p.getProcessorActivityList().stream())
                    .filter(pa -> pa.getProcessActivityInfo() != null)
                    .flatMap(pa -> pa.getProcessActivityInfo().getDataTypesList().stream())
                    .filter(dt -> dt.getDataItems() != null)
                    .flatMap(dt -> dt.getDataItems().stream())
                    .collect(Collectors.toSet());

            logDataItems("extractedDataItems from withdrawn consent", dataItems);
            return dataItems;
        } catch (Exception e) {
            log.error("Error while extracting withdrawn consent data items. consentId={}",
                    consent != null ? consent.getConsentId() : null,
                    e);
            return Set.of();
        }
    }
    /*
        gets the set of all the dataitems from the active consents of the user
     */
    public Map<String, Set<String>> getActiveConsents(Consent consent, String tenantId, String businessId) {
        try {
            if (consent == null || consent.getCustomerIdentifiers() == null) {
                return Map.of();
            }

            MongoTemplate mongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            String identifier = consent.getCustomerIdentifiers().getValue();

            Query query = new Query();
            query.addCriteria(Criteria.where("customerIdentifiers.value").is(identifier));
            query.addCriteria(Criteria.where("businessId").is(businessId));
            query.addCriteria(Criteria.where("status").is("ACTIVE"));
            query.addCriteria(Criteria.where("staleStatus").is("NOT_STALE"));
            query.addCriteria(Criteria.where("consentId").ne(consent.getConsentId()));
            log.debug(query.toString());

            List<Consent> activeConsents = mongoTemplate.find(query, Consent.class);
            Map<String, Set<String>> activeItemToConsentIds = new HashMap<>();

            for (Consent c : activeConsents) {
                String consentId = c.getConsentId().toString();

                if (c.getPreferences() == null) continue;

                c.getPreferences().stream()
                    .filter(p -> p.getProcessorActivityList() != null)
                    .flatMap(p -> p.getProcessorActivityList().stream())
                    .filter(pa -> pa.getProcessActivityInfo() != null)
                    .flatMap(pa -> pa.getProcessActivityInfo().getDataTypesList().stream())
                    .filter(dt -> dt.getDataItems() != null)
                    .flatMap(dt -> dt.getDataItems().stream())
                    .forEach(item ->
                        activeItemToConsentIds
                            .computeIfAbsent(item, k -> new HashSet<>())
                            .add(consentId)
                    );
            }

            logDataItems("extractedDataItems from ACTIVE & NOT_STALE consents", activeItemToConsentIds.keySet());
            return activeItemToConsentIds;
        } catch (Exception e) {
            log.error("Error while extracting active consent data items. tenantId={}, businessId={}",
                    tenantId,
                    businessId,
                    e);
            return Map.of();
        }
    }

    private void logDataItems(String context, Set<String> dataItems) {
        if (dataItems == null || dataItems.isEmpty()) {
            log.info("[{}] DataItems: EMPTY", context);
            return;
        }
        log.info("[{}] DataItems count={}, values={}", context, dataItems.size(), dataItems);
    }
}