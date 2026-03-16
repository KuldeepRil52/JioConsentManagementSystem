package com.jio.partnerportal.service;

import com.jio.partnerportal.client.notification.NotificationManager;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest;
import com.jio.partnerportal.client.notification.request.TriggerEventRequest.CustomerIdentifiers;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.IdentityType;
import com.jio.partnerportal.dto.response.DataBreachNotifyResponse;
import com.jio.partnerportal.entity.BusinessApplication;
import com.jio.partnerportal.entity.DPOConfig;
import com.jio.partnerportal.entity.DataBreachNotify;
import com.jio.partnerportal.entity.DataBreachReport;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.multitenancy.TenantMongoTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DataBreachNotifierService
 *
 * - Reads the DataBreachReport from tenant collection "data_breach_reports"
 * - Extracts personalDataCategories
 * - Updates the DataBreachReport.notificationDetails.status to IN_PROGRESS
 * - Finds matching consents from "consents" collection
 * - Persists a data_breach_notify document with list of consents and customer identifiers (status PENDING)
 * - Triggers notification events via NotificationManager.triggerEventAsync
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataBreachNotifierService {

    private final TenantMongoTemplateProvider tenantMongoTemplateProvider;
    private final NotificationManager notificationManager;

    /**
     * Processes a data breach incident and triggers notifications per consent (one record per consent).
     */
    public DataBreachNotifyResponse processAndNotify(String tenantId, String incidentId) throws PartnerPortalException {
        validateInputs(tenantId, incidentId);

        ThreadContext.put(Constants.TENANT_ID_HEADER, tenantId);
        MongoTemplate tenantMongo = tenantMongoTemplateProvider.getMongoTemplate(tenantId);

        // Fetch incident
        DataBreachReport incident = tenantMongo.findOne(
                Query.query(Criteria.where("incidentId").is(incidentId)),
                DataBreachReport.class,
                "data_breach_reports");

        if (incident == null) {
            log.warn("Incident not found for id={} tenant={}", incidentId, tenantId);
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        // Skip if already notified
        if (incident.getNotificationDetails() != null &&
                incident.getNotificationDetails().getStatus() != null &&
                !incident.getNotificationDetails().getStatus().isBlank()) {
            throw new PartnerPortalException(ErrorCodes.JCMP3058);
        }

        // Build query based on personalDataCategories
        List<String> personalDataCategories = Optional.ofNullable(incident.getDataInvolved())
                .map(DataBreachReport.DataInvolved::getPersonalDataCategories)
                .orElse(Collections.emptyList());

        if (personalDataCategories.isEmpty()) {
            log.warn("No personal data categories found in incident={} for tenant={}", incidentId, tenantId);
           throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }

        Criteria consentCriteria = Criteria.where("preferences.processorActivityList.processActivityInfo.dataTypesList.dataItems")
                .in(personalDataCategories);

        Query consentQuery = new Query();
        consentQuery.addCriteria(consentCriteria).fields().include("consentId").include("customerIdentifiers").include("businessId");

        log.info("Found consents matching personalDataCategories={} for incident={} tenant={}",
                personalDataCategories, incidentId, tenantId);

        // Shared notifyGroupId for this incident run
        String notifyGroupId = UUID.randomUUID().toString();
        Object lastId = null;
        int batchSize = 500;
        String collection = "consents";

        while (true) {
            Query query = new Query(consentCriteria);
            if (lastId != null) query.addCriteria(Criteria.where("_id").gt(lastId));
            query.with(Sort.by(Sort.Direction.ASC, "_id"));
            query.limit(batchSize);

            List<Document> consents = tenantMongo.find(query, Document.class, collection);
            if (consents.isEmpty()) break;

            List<DataBreachNotify> notifyBatch = new ArrayList<>();

            for (Document doc : consents) {
                String consentId = doc.getString("consentId");
                String businessId = doc.getString("businessId");

                //Fetch DPO Details
                DPOConfig dpoConfig = tenantMongo.findOne(
                        Query.query(Criteria.where("businessId").is(businessId)),
                        DPOConfig.class,
                        "dpo_configurations");

                //Fetch business application details
                BusinessApplication businessApp = tenantMongo.findOne(
                        Query.query(Criteria.where("businessId").is(businessId)),
                        BusinessApplication.class,
                        "business_applications");

                if (consentId == null || businessId == null) continue;

                TriggerEventRequest.CustomerIdentifiers identifiers = extractIdentifiers(doc.get("customerIdentifiers"));
                if (identifiers == null) continue;

                DataBreachNotify notify = DataBreachNotify.builder()
                        .notifyId(UUID.randomUUID().toString())
                        .notifyGroupId(notifyGroupId)
                        .tenantId(tenantId)
                        .incidentId(incidentId)
                        .consent(DataBreachNotify.ConsentEntry.builder()
                                .consentId(consentId)
                                .businessId(businessId)
                                .customerIdentifiers(identifiers)
                                .status(DataBreachReport.NotificationStatus.PENDING)
                                .build())
                        .status(String.valueOf(DataBreachReport.NotificationStatus.PENDING))
                        .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                        .build();

                log.debug("Prepared data_breach_notify record for consent={} incident={} tenant={}",
                        consentId, incidentId, tenantId);

                notifyBatch.add(notify);

                log.debug("Triggering async notification for consent={} incident={} tenant={}",
                        consentId, incidentId, tenantId);

                triggerAsyncNotification(notify, tenantId, businessId, consentId, personalDataCategories, incident, dpoConfig, businessApp);

                log.debug("Triggered async notification for consent={} incident={} tenant={}",
                        consentId, incidentId, tenantId);
            }

            if (!notifyBatch.isEmpty()) {
                log.info("Inserting {} data_breach_notify records for incident={} tenant={}",
                        notifyBatch.size(), incidentId, tenantId);
                tenantMongo.bulkOps(BulkOperations.BulkMode.UNORDERED, "data_breach_notify")
                        .insert(notifyBatch)
                        .execute();
            }

            lastId = consents.get(consents.size() - 1).getObjectId("_id");
        }

        log.info("Completed processing data breach notifications for incident={} tenant={}",
                incidentId, tenantId);

        // Update incident status to IN_PROGRESS
        updateIncidentStatus(tenantMongo, incident, notifyGroupId);

        log.info("Updated incident={} status to IN_PROGRESS tenant={}",
                incidentId, tenantId);

        return new DataBreachNotifyResponse(
                incidentId,
                tenantId,
                notifyGroupId,
                LocalDateTime.now(ZoneOffset.UTC),
                "IN_PROGRESS"
        );
    }

    private void validateInputs(String tenantId, String incidentId) throws PartnerPortalException {
        if (tenantId == null || tenantId.isBlank() || incidentId == null || incidentId.isBlank()) {
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
    }

    private void updateIncidentStatus(MongoTemplate tenantMongo, DataBreachReport incident, String notifyGroupId) {
        try {
            if (incident.getNotificationDetails() == null) {
                incident.setNotificationDetails(new DataBreachReport.NotificationDetails());
            }
            incident.getNotificationDetails().setStatus("IN_PROGRESS");
            incident.getNotificationDetails().setNotifyGroupId(notifyGroupId);
            incident.getNotificationDetails().setUpdatedAt(LocalDateTime.now());
            tenantMongo.save(incident, "data_breach_reports");

            log.info("Updated notificationDetails for incident={} (notifyGroupId={})",
                    incident.getIncidentId(), notifyGroupId);
        } catch (Exception e) {
            log.error("Failed to update incident notificationDetails: {}", e.getMessage(), e);
        }
    }

    private void triggerAsyncNotification(
            DataBreachNotify notify,
            String tenantId,
            String businessId,
            String consentId,
            List<String> categories,
            DataBreachReport incident,
            DPOConfig dpoConfig,
            BusinessApplication businessApp) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        ZonedDateTime breachDate = incident.getIncidentDetails().getOccurrenceDateTime()        // LocalDateTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

        ZonedDateTime reportedToAuthDate = incident.getNotificationDetails().getDpbiNotificationDate()        // LocalDateTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Kolkata"));

        try {
            TriggerEventRequest event = TriggerEventRequest.builder()
                    .eventType("DATA_BREACHED")
                    .resource("data_breach")
                    .language("en")
                    .customerIdentifiers(notify.getConsent().getCustomerIdentifiers())
                    .eventPayload(Map.of(
                            "consentId", consentId,
                            "businessId", businessId,
                            "breachDate", breachDate.format(fmt),
                            "breachReferenceId", incident.getIncidentId(),
                            "affectedDataCategory", categories,
                            "reportedBy", businessApp.getName(),
                            "reportedToAuthorityDate", reportedToAuthDate.format(fmt),
                            "dpoContactName", dpoConfig.getConfigurationJson().getName(),
                            "dpoContactDetails",dpoConfig.getConfigurationJson().getEmail()
                    ))
                    .build();

            notificationManager.triggerEventAsync(tenantId, businessId, "TENANT",
                    event, notify.getNotifyId(), notify.getNotifyGroupId(), consentId);

        } catch (Exception e) {
            log.error("Failed to trigger notification for tenant={} consent={} : {}",
                    tenantId, consentId, e.getMessage());
        }
    }

    /**
     * Extracts customer identifiers safely.
     */
    private static CustomerIdentifiers extractIdentifiers(Object idens) {
        if (idens instanceof Map<?, ?> single) {
            Object typeObj = single.get("type");
            Object valueObj = single.get("value");
            if (typeObj == null || valueObj == null) return null;

            try {
                IdentityType type = IdentityType.valueOf(String.valueOf(typeObj).toUpperCase(Locale.ROOT));
                return CustomerIdentifiers.builder()
                        .type(type)
                        .value(String.valueOf(valueObj))
                        .build();
            } catch (IllegalArgumentException e) {
                return null; // invalid type
            }
        }
        return null;
    }
}
