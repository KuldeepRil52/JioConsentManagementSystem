package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.request.DataBreachReportRequest;
import com.jio.partnerportal.dto.request.DataBreachUpdateRequest;
import com.jio.partnerportal.dto.response.DataBreachReportResponse;
import com.jio.partnerportal.dto.response.DataBreachReportSimpleResponse;
import com.jio.partnerportal.entity.DataBreachReport;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.DataBreachReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import com.jio.partnerportal.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Slf4j
@Service
public class DataBreachReportService {

    private final DataBreachReportRepository repository;
    private final AuditManager auditManager;

    @Autowired
    public DataBreachReportService(DataBreachReportRepository repository, AuditManager auditManager) {
        this.repository = repository;
        this.auditManager = auditManager;
    }

    public DataBreachReport create(String tenantId, DataBreachReportRequest request, HttpServletRequest req) throws PartnerPortalException {

        String activity = "Create Data Breach Report";

        DataBreachReport entity = mapToEntity(request);
        entity.setTenantId(tenantId);
        entity.setStatus(DataBreachReport.BreachStatus.NEW);
        
        // Generate incident ID and set at root level
        String incidentId = generateIncidentId(tenantId);
        entity.setIncidentId(incidentId);
        
        // Initialize history with first entry (previousStatus is null)
        List<DataBreachReport.StatusHistory> history = new ArrayList<>();
        history.add(DataBreachReport.StatusHistory.builder()
                .previousStatus(null)
                .newStatus(DataBreachReport.BreachStatus.NEW)
                .updatedAt(LocalDateTime.now())
                .remarks(null)
                .build());
        entity.setHistory(history);
        
        // Pre-fill notification details with random data
        entity.setNotificationDetails(generateRandomNotificationDetails(entity.getDataInvolved()));

        this.logDataBreachReportAudit(entity, ActionType.CREATE);
        LogUtil.logActivity(req, activity, "Success: Create Data Breach Report successfully");
        return repository.save(entity);
    }

    public DataBreachReport update(String tenantId, String id, DataBreachUpdateRequest request,HttpServletRequest req) throws PartnerPortalException {

        String activity = "Update Data breach report";

        DataBreachReport existing = repository.findById(id);
        if (existing == null) {
            throw new PartnerPortalException("JCMP3001");
        }

        if (!existing.getTenantId().equals(tenantId)) {
            throw new PartnerPortalException("JCMP3001");
        }

        DataBreachReport.BreachStatus currentStatus = existing.getStatus();
        DataBreachReport.BreachStatus newStatus = request.getStatus();

        if (newStatus != null) {
            // Check if trying to update to the same status
            if (currentStatus.equals(newStatus)) {
                throw new PartnerPortalException(ErrorCodes.JCMP3046);
            }

            if (!isValidStatusTransition(currentStatus, newStatus)) {
                throw new PartnerPortalException(ErrorCodes.JCMP3046);
            }

            // Status will be different at this point (same status check already thrown error above)
            DataBreachReport.StatusHistory historyEntry = DataBreachReport.StatusHistory.builder()
                    .previousStatus(currentStatus)
                    .newStatus(newStatus)
                    .updatedAt(LocalDateTime.now())
                    .remarks(request.getRemarks())
                    .build();

            if (existing.getHistory() == null) {
                existing.setHistory(new ArrayList<>());
            }
            existing.getHistory().add(historyEntry);
            existing.setStatus(newStatus);
        }

        this.logDataBreachReportAudit(existing, ActionType.UPDATE);
        LogUtil.logActivity(req, activity, "Success: Update Data breach report successfully");
        return repository.save(existing);
    }

    public DataBreachReport getById(String tenantId, String id) throws PartnerPortalException {
        DataBreachReport report = repository.findById(id);
        if (report == null) {
            throw new PartnerPortalException("JCMP3001");
        }
        if (!report.getTenantId().equals(tenantId)) {
            throw new PartnerPortalException("JCMP3001");
        }
        return report;
    }

    public List<DataBreachReport> getAll(String tenantId) {
        return repository.findAllByTenantId(tenantId);
    }

    private String generateIncidentId(String tenantId) {
        int currentYear = Year.now().getValue();
        String prefix = "DB-" + currentYear + "-";
        
        // Fetch the latest entry by createdAt date
        DataBreachReport latestReport = repository.findLatestByTenantId(tenantId);
        
        if (latestReport == null || latestReport.getIncidentId() == null) {
            // No entries exist or latest entry doesn't have incidentId, start with 001
            return prefix + "001";
        }
        
        String latestIncidentId = latestReport.getIncidentId();
        
        // Check if the latest entry's ID contains the current year prefix
        if (latestIncidentId.startsWith(prefix)) {
            // Extract sequence number and increment it
            try {
                String sequenceStr = latestIncidentId.substring(prefix.length());
                int sequence = Integer.parseInt(sequenceStr);
                int newSequence = sequence + 1;
                return prefix + String.format("%03d", newSequence);
            } catch (NumberFormatException e) {
                // Invalid format, start with 001 for current year
                return prefix + "001";
            }
        } else {
            // Latest entry has different year prefix, create new ID starting from 001
            return prefix + "001";
        }
    }

    private boolean isValidStatusTransition(DataBreachReport.BreachStatus currentStatus, DataBreachReport.BreachStatus newStatus) {
        // Allow same status (for remarks-only updates)
        if (currentStatus == newStatus) {
            return true;
        }

        DataBreachReport.BreachStatus[] statusOrder = {
                DataBreachReport.BreachStatus.NEW,
                DataBreachReport.BreachStatus.INVESTIGATION,
                DataBreachReport.BreachStatus.NOTIFIED_TO_DATA_PRINCIPALS,
                DataBreachReport.BreachStatus.NOTIFIED_TO_DPBI,
                DataBreachReport.BreachStatus.RESOLVED
        };

        int currentIndex = -1;
        int newIndex = -1;

        for (int i = 0; i < statusOrder.length; i++) {
            if (statusOrder[i] == currentStatus) {
                currentIndex = i;
            }
            if (statusOrder[i] == newStatus) {
                newIndex = i;
            }
        }

        if (currentIndex == -1 || newIndex == -1) {
            return false;
        }

        // Status can only move to the next sequential status
        return newIndex == currentIndex + 1;
    }

    public DataBreachReportResponse mapToResponse(DataBreachReport entity) {
        if (entity == null) {
            return null;
        }

        DataBreachReportResponse.DataInvolved dataInvolved = null;
        if (entity.getDataInvolved() != null) {
            dataInvolved = DataBreachReportResponse.DataInvolved.builder()
                    .personalDataCategories(entity.getDataInvolved().getPersonalDataCategories())
                    .affectedDataPrincipalsCount(entity.getDataInvolved().getAffectedDataPrincipalsCount())
                    .dataEncryptedOrProtected(entity.getDataInvolved().getDataEncryptedOrProtected())
                    .potentialImpactDescription(entity.getDataInvolved().getPotentialImpactDescription())
                    .build();
        }

        DataBreachReportResponse.IncidentDetails incidentDetails = null;
        if (entity.getIncidentDetails() != null) {
            incidentDetails = DataBreachReportResponse.IncidentDetails.builder()
                    .discoveryDateTime(entity.getIncidentDetails().getDiscoveryDateTime())
                    .occurrenceDateTime(entity.getIncidentDetails().getOccurrenceDateTime())
                    .breachType(entity.getIncidentDetails().getBreachType())
                    .briefDescription(entity.getIncidentDetails().getBriefDescription())
                    .affectedSystemOrService(entity.getIncidentDetails().getAffectedSystemOrService())
                    .build();
        }

        List<DataBreachReportResponse.StatusHistory> historyList = null;
        if (entity.getHistory() != null) {
            historyList = entity.getHistory().stream()
                    .map(history -> DataBreachReportResponse.StatusHistory.builder()
                            .previousStatus(history.getPreviousStatus())
                            .newStatus(history.getNewStatus())
                            .updatedAt(history.getUpdatedAt())
                            .remarks(history.getRemarks())
                            .build())
                    .toList();
        }

        DataBreachReportResponse.NotificationDetails notificationDetails = null;
        if (entity.getNotificationDetails() != null) {
            List<DataBreachReportResponse.NotificationDetails.NotificationChannel> channels = null;
            if (entity.getNotificationDetails().getChannels() != null) {
                channels = entity.getNotificationDetails().getChannels().stream()
                        .map(channel -> DataBreachReportResponse.NotificationDetails.NotificationChannel.builder()
                                .notificationChannel(channel.getNotificationChannel())
                                .notificationStatus(channel.getNotificationStatus())
                                .count(channel.getCount())
                                .build())
                        .toList();
            }

            notificationDetails = DataBreachReportResponse.NotificationDetails.builder()
                    .dpbiNotificationDate(entity.getNotificationDetails().getDpbiNotificationDate())
                    .dpbiAcknowledgementId(entity.getNotificationDetails().getDpbiAcknowledgementId())
                    .dataPrincipalNotificationDate(entity.getNotificationDetails().getDataPrincipalNotificationDate())
                    .channels(channels)
                    .build();
        }
        return DataBreachReportResponse.builder()
                .id(entity.get_id() != null ? entity.get_id().toHexString() : null)
                .tenantId(entity.getTenantId())
                .incidentId(entity.getIncidentId())
                .incidentDetails(incidentDetails)
                .dataInvolved(dataInvolved)
                .status(entity.getStatus())
                .history(historyList)
                .notificationDetails(notificationDetails)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private DataBreachReport mapToEntity(DataBreachReportRequest request) {
        DataBreachReport entity = new DataBreachReport();

        if (request.getIncidentDetails() != null) {
            entity.setIncidentDetails(DataBreachReport.IncidentDetails.builder()
                    .discoveryDateTime(request.getIncidentDetails().getDiscoveryDateTime())
                    .occurrenceDateTime(request.getIncidentDetails().getOccurrenceDateTime())
                    .breachType(request.getIncidentDetails().getBreachType())
                    .briefDescription(request.getIncidentDetails().getBriefDescription())
                    .affectedSystemOrService(request.getIncidentDetails().getAffectedSystemOrService())
                    .build());
        }

        if (request.getDataInvolved() != null) {
            entity.setDataInvolved(DataBreachReport.DataInvolved.builder()
                    .personalDataCategories(request.getDataInvolved().getPersonalDataCategories())
                    .affectedDataPrincipalsCount(request.getDataInvolved().getAffectedDataPrincipalsCount())
                    .dataEncryptedOrProtected(request.getDataInvolved().getDataEncryptedOrProtected())
                    .potentialImpactDescription(request.getDataInvolved().getPotentialImpactDescription())
                    .build());
        }

        return entity;
    }

    private DataBreachReport.NotificationDetails generateRandomNotificationDetails(DataBreachReport.DataInvolved dataInvolved) {
        SecureRandom secureRandom = new SecureRandom();
        LocalDateTime now = LocalDateTime.now();
        
        // Generate random DPBI acknowledgement ID
        String dpbiAckId = "DPBI-ACK-2025-" + String.format("%04d", secureRandom.nextInt(10000));
        
        // Generate random notification dates (future dates)
        LocalDateTime dpbiNotificationDate = now.plusDays(secureRandom.nextInt(3) + 1);
        LocalDateTime dataPrincipalNotificationDate = dpbiNotificationDate.plusHours(secureRandom.nextInt(6) + 1);
        
        // Generate channels with count 0
        List<DataBreachReport.NotificationDetails.NotificationChannel> channels = new ArrayList<>();
        
        // Add EMAIL channel
        channels.add(DataBreachReport.NotificationDetails.NotificationChannel.builder()
                .notificationChannel(DataBreachReport.NotificationChannelType.EMAIL)
                .notificationStatus(DataBreachReport.NotificationStatus.SENT)
                .count(0)
                .build());
        
        // Add SMS channel
        channels.add(DataBreachReport.NotificationDetails.NotificationChannel.builder()
                .notificationChannel(DataBreachReport.NotificationChannelType.SMS)
                .notificationStatus(DataBreachReport.NotificationStatus.SENT)
                .count(0)
                .build());
        
        return DataBreachReport.NotificationDetails.builder()
                .dpbiNotificationDate(dpbiNotificationDate)
                .dpbiAcknowledgementId(dpbiAckId)
                .dataPrincipalNotificationDate(dataPrincipalNotificationDate)
                .channels(channels)
                .build();
    }

    public DataBreachReportSimpleResponse mapToSimpleResponse(DataBreachReport entity) {
        if (entity == null) {
            return null;
        }

        return DataBreachReportSimpleResponse.builder()
                .incidentId(entity.getIncidentId())
                .status(entity.getStatus())
                .build();
    }

    /**
     * Modular function to log data breach report audit events
     * Can be used in both create and update data breach report flows
     *
     * @param dataBreachReport The data breach report entity to audit
     * @param actionType The action type (CREATE, UPDATE)
     */
    public void logDataBreachReportAudit(DataBreachReport dataBreachReport, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.INCIDENT_ID)
                    .id(dataBreachReport.getIncidentId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add data breach report POJO in the extra field under the "data" key
            extra.put(Constants.DATA, dataBreachReport);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.DATA_BREACH_REPORT)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for incident id: {}, action: {}, error: {}", 
                    dataBreachReport.getIncidentId(), actionType, e.getMessage(), e);
        }
    }
}
