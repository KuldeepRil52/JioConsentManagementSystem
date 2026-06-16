package com.jio.digigov.notification.service.impl;

import com.jio.digigov.notification.config.MultiTenantMongoConfig;
import com.jio.digigov.notification.constant.HeaderConstants;
import com.jio.digigov.notification.enums.NotificationType;
import com.jio.digigov.notification.enums.IdType;
import com.jio.digigov.notification.entity.NotificationAudit;
import com.jio.digigov.notification.service.AuditService;
import com.jio.digigov.notification.util.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final MultiTenantMongoConfig mongoConfig;
    private final AtomicLong auditCounter = new AtomicLong(System.currentTimeMillis() % 1000000);

    @Override
    public void createAudit(String txnId, String templateId, String notificationType, 
                           String eventType, IdType idType, String idValue, String status,
                           Object request, Object response, String errorMessage, Long responseTime) {
        
        try {
            String tenantId = TenantContextHolder.getTenantId();
            String businessId = TenantContextHolder.getBusinessId();
            
            NotificationAudit audit = NotificationAudit.builder()
                    .auditId(generateAuditId())
                    .tenantId(tenantId)
                    .businessId(businessId)
                    .txnId(txnId)
                    .templateId(templateId)
                    .eventType(eventType)
                    .status(status)
                    .operation(extractOperationFromEventType(eventType))
                    .requestData(convertToMap(request))
                    .responseData(convertToMap(response))
                    .errorMessage(errorMessage)
                    .processingTimeMs(responseTime)
                    .build();

            // Save to tenant-specific database
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            tenantMongoTemplate.save(audit);
            log.debug("Audit created: {}", audit.getAuditId());
            
        } catch (Exception e) {
            log.error("Failed to create audit for txnId: {}", txnId);
            // Don't throw exception as audit failure shouldn't break the main flow
        }
    }

    @Override
    public NotificationAudit getAuditByTxnId(String txnId) {
        try {
            String tenantId = TenantContextHolder.getTenantId();
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            
            Query query = new Query(Criteria.where("txnId").is(txnId));
            NotificationAudit audit = tenantMongoTemplate.findOne(query, NotificationAudit.class);
            
            if (audit == null) {
                throw new RuntimeException("Audit not found for txnId: " + txnId);
            }
            
            return audit;
        } catch (Exception e) {
            log.error("Failed to get audit for txnId: {}", txnId);
            throw e;
        }
    }

    @Override
    public void auditTemplateCreation(String templateId, String templateType, Object request, Object response) {
        createAudit(
                UUID.randomUUID().toString(),
                templateId,
                templateType,
                "TEMPLATE_CREATED",
                null,
                null,
                "SUCCESS",
                request,
                response,
                null,
                null
        );
    }

    @Override
    public void auditOTPInit(String txnId, String templateId, IdType idType, String idValue, 
                            Object request, Object response, String status, Long responseTime) {
        String notificationType = (idType == IdType.PHONE) ? "SMS" : "EMAIL";
        
        createAudit(
                txnId,
                templateId,
                notificationType,
                "OTP_INIT",
                idType,
                maskSensitiveData(idValue, idType),
                status,
                request,
                response,
                null,
                responseTime
        );
    }

    @Override
    public void auditOTPVerify(String txnId, Object request, Object response, 
                              String status, Long responseTime) {
        createAudit(
                txnId,
                null,
                "OTP",
                "OTP_VERIFY",
                null,
                null,
                status,
                maskOTPFromRequest(request),
                response,
                null,
                responseTime
        );
    }

    @Override
    public String generateAuditId() {
        long counter = auditCounter.incrementAndGet();
        return "AUDIT_" + String.format("%010d", counter);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        
        // For simple objects, create a basic map representation
        Map<String, Object> map = new HashMap<>();
        map.put("data", obj);
        map.put("type", obj.getClass().getSimpleName());
        return map;
    }

    private String maskSensitiveData(String value, IdType idType) {
        if (value == null || value.length() <= 4) {
            return value;
        }
        
        if (idType == IdType.PHONE) {
            // Mask phone number: 9999999999 -> 999***9999
            return value.substring(0, 3) + "***" + value.substring(value.length() - 4);
        } else if (idType == IdType.EMAIL) {
            // Mask email: user@example.com -> u***@example.com
            int atIndex = value.indexOf('@');
            if (atIndex > 0) {
                return value.charAt(0) + "***" + value.substring(atIndex);
            }
        }
        
        return value;
    }

    private Object maskOTPFromRequest(Object request) {
        if (request instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestMap = new HashMap<>((Map<String, Object>) request);
            if (requestMap.containsKey("userOTP")) {
                requestMap.put("userOTP", "***MASKED***");
            }
            return requestMap;
        }
        return request;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                String xForwardedFor = request.getHeader(HeaderConstants.X_FORWARDED_FOR);
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIP = request.getHeader(HeaderConstants.X_REAL_IP);
                if (xRealIP != null && !xRealIP.isEmpty()) {
                    return xRealIP;
                }
                
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine client IP address: {}", e.getMessage());
        }
        
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader(HeaderConstants.USER_AGENT);
            }
        } catch (Exception e) {
            log.debug("Could not determine user agent: {}", e.getMessage());
        }
        
        return "unknown";
    }

    // Comprehensive audit methods for step-by-step operation tracking

    @Override
    public void auditTemplateOperation(String templateId, String operationType, String step, 
                                      Map<String, String> headers, Object payload, String message) {
        try {
            String txnId = headers != null ? headers.get("txn") : UUID.randomUUID().toString();
            
            createAudit(
                txnId,
                templateId,
                operationType,
                "TEMPLATE_" + step,
                null, // No specific ID type for template operations
                "TEMPLATE_OPERATION", // Default ID value for template operations
                step.contains("SUCCESS") ? "SUCCESS" : step.contains("FAILED") ? "FAILED" : "PROCESSING",
                extractContextFromHeaders(headers),
                maskSensitiveData(payload),
                message,
                null
            );
            
            log.debug("Template operation audited: {} - {} - {}", templateId, operationType, step);
            
        } catch (Exception e) {
            log.error("Failed to audit template operation: {} - {}", templateId, step);
        }
    }

    @Override
    public void auditConfigurationOperation(String configId, String operation, 
                                          Map<String, String> headers, Object payload, 
                                          String status, String message) {
        try {
            String txnId = headers != null ? headers.get("txn") : UUID.randomUUID().toString();
            
            createAudit(
                txnId,
                configId,
                "CONFIGURATION",
                "CONFIG_" + operation,
                null, // No specific ID type for configuration operations
                "CONFIG_OPERATION", // Default ID value for configuration operations
                status,
                extractContextFromHeaders(headers),
                maskSensitiveData(payload),
                message,
                null
            );
            
            log.debug("Configuration operation audited: {} - {} - {}", configId, operation, status);
            
        } catch (Exception e) {
            log.error("Failed to audit configuration operation: {} - {}", configId, operation);
        }
    }

    @Override
    public void auditNotificationSending(String templateId, String recipient, String channel,
                                        Map<String, String> headers, Object request, Object response,
                                        String status, Long responseTime) {
        try {
            String txnId = headers != null ? headers.get("txn") : UUID.randomUUID().toString();
            IdType idType = "EMAIL".equals(channel) ? IdType.EMAIL : IdType.PHONE;
            
            createAudit(
                txnId,
                templateId,
                channel,
                "NOTIFICATION_SEND",
                idType,
                maskRecipient(recipient, channel),
                status,
                maskSensitiveData(request),
                maskSensitiveData(response),
                null,
                responseTime
            );
            
            log.debug("Notification sending audited: {} - {} - {}", templateId, channel, status);
            
        } catch (Exception e) {
            log.error("Failed to audit notification sending: {} - {}", templateId, channel);
        }
    }

    @Override
    public void auditTokenGeneration(String tenantId, String businessId, String networkType,
                                    Map<String, String> headers, String status, String message) {
        try {
            String txnId = headers != null ? headers.get("txn") : UUID.randomUUID().toString();
            
            Map<String, Object> context = new HashMap<>();
            context.put("tenantId", tenantId);
            context.put("businessId", businessId);
            context.put("networkType", networkType);
            if (headers != null) {
                context.putAll(headers);
            }
            
            createAudit(
                txnId,
                null,
                "TOKEN",
                "TOKEN_GENERATION",
                null,
                null,
                status,
                context,
                null,
                message,
                null
            );
            
            log.debug("Token generation audited: {} - {} - {}", tenantId, networkType, status);
            
        } catch (Exception e) {
            log.error("Failed to audit token generation: {} - {}", tenantId, networkType);
        }
    }

    @Override
    public List<NotificationAudit> getAuditsByTemplateId(String templateId) {
        try {
            String tenantId = TenantContextHolder.getTenantId();
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            
            Query query = new Query(Criteria.where("templateId").is(templateId))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            
            return tenantMongoTemplate.find(query, NotificationAudit.class);
        } catch (Exception e) {
            log.error("Failed to get audits for template: {}", templateId);
            return List.of();
        }
    }

    @Override
    public List<NotificationAudit> getAuditsByOperationType(String operationType, int limit) {
        try {
            String tenantId = TenantContextHolder.getTenantId();
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            
            Query query = new Query(Criteria.where("eventType").regex("^" + operationType))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .limit(limit);
            
            return tenantMongoTemplate.find(query, NotificationAudit.class);
        } catch (Exception e) {
            log.error("Failed to get audits for operation type: {}", operationType);
            return List.of();
        }
    }

    @Override
    public List<NotificationAudit> getAuditsByTenantAndDateRange(String tenantId, String fromDate, String toDate) {
        try {
            LocalDateTime from = LocalDateTime.parse(fromDate + "T00:00:00");
            LocalDateTime to = LocalDateTime.parse(toDate + "T23:59:59");
            
            MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
            
            Query query = new Query(Criteria.where("tenantId").is(tenantId)
                    .and("createdAt").gte(from).lte(to))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"));
            
            return tenantMongoTemplate.find(query, NotificationAudit.class);
        } catch (Exception e) {
            log.error("Failed to get audits for tenant {} between {} and {}", tenantId, fromDate, toDate);
            return List.of();
        }
    }

    @Override
    public Object maskSensitiveData(Object payload) {
        if (payload == null) {
            return null;
        }

        if (payload instanceof Map) {
            return maskSensitiveFieldsInMap(payload);
        }

        return payload;
    }

    private Map<String, Object> maskSensitiveFieldsInMap(Object payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payloadMap = new HashMap<>((Map<String, Object>) payload);

        getSensitiveFieldNames().forEach(fieldName -> {
            if (payloadMap.containsKey(fieldName)) {
                payloadMap.put(fieldName, "***MASKED***");
            }
        });

        return payloadMap;
    }

    private List<String> getSensitiveFieldNames() {
        return List.of("client_secret", "password", "token", "userOTP", "otp", "mutualCertificate");
    }

    @Override
    public long archiveOldAudits(int olderThanDays) {
        try {
            String tenantId = TenantContextHolder.getTenantId();
            List<NotificationAudit> oldAudits = findOldAudits(tenantId, olderThanDays);
            long archivedCount = archiveAuditRecords(tenantId, oldAudits);

            log.info("Archived {} audit records older than {} days", archivedCount, olderThanDays);
            return archivedCount;

        } catch (Exception e) {
            log.error("Failed to archive old audits");
            return 0;
        }
    }

    private List<NotificationAudit> findOldAudits(String tenantId, int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);
        Query query = new Query(Criteria.where("createdAt").lt(cutoffDate));
        return tenantMongoTemplate.find(query, NotificationAudit.class);
    }

    private long archiveAuditRecords(String tenantId, List<NotificationAudit> oldAudits) {
        MongoTemplate tenantMongoTemplate = mongoConfig.getMongoTemplateForTenant(tenantId);

        // In a full implementation, these would be moved to an archive collection
        // For now, we'll just mark them as archived by updating the document
        for (NotificationAudit audit : oldAudits) {
            audit.setUpdatedAt(LocalDateTime.now());
            tenantMongoTemplate.save(audit);
        }

        return oldAudits.size();
    }

    // Helper methods for comprehensive auditing

    private Map<String, Object> extractContextFromHeaders(Map<String, String> headers) {
        Map<String, Object> context = new HashMap<>();
        
        if (headers != null) {
            context.put("tenantId", headers.get("tenantId"));
            context.put("businessId", headers.get("businessId"));
            context.put("scopeLevel", headers.get("scopeLevel"));
            context.put("type", headers.get("type"));
            context.put("txn", headers.get("txn"));
            // Remove null values
            context.entrySet().removeIf(entry -> entry.getValue() == null);
        }
        
        return context;
    }

    private String maskRecipient(String recipient, String channel) {
        if (recipient == null || recipient.length() <= 4) {
            return recipient;
        }
        
        if ("EMAIL".equals(channel)) {
            // Mask email: user@example.com -> u***@example.com
            int atIndex = recipient.indexOf('@');
            if (atIndex > 0) {
                return recipient.charAt(0) + "***" + recipient.substring(atIndex);
            }
        } else {
            // Mask phone: 9999999999 -> 999***9999
            return recipient.substring(0, 3) + "***" + recipient.substring(recipient.length() - 4);
        }
        
        return recipient;
    }

    private String extractOperationFromEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        
        if (eventType.contains("TEMPLATE")) {
            return "template";
        } else if (eventType.contains("OTP")) {
            return "otp";
        } else if (eventType.contains("CONFIG")) {
            return "config";
        } else if (eventType.contains(NotificationType.NOTIFICATION.name())) {
            return "notification";
        }
        
        return eventType.toLowerCase();
    }
}