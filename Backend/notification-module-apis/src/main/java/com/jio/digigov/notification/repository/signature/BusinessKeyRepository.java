package com.jio.digigov.notification.repository.signature;

import com.jio.digigov.notification.util.MongoTemplateProvider;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for retrieving business certificates used for signature verification.
 *
 * <p>This repository provides methods to fetch public key certificates from:
 * <ul>
 *   <li><b>notification_configurations</b> collection - for DATA_FIDUCIARY entities</li>
 *   <li><b>data_processors</b> collection - for DATA_PROCESSOR entities</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // For DATA_FIDUCIARY
 * String certificate = businessKeyRepository.findCertificateByBusinessId(tenantId, businessId);
 *
 * // For DATA_PROCESSOR
 * String certificate = businessKeyRepository.findCertificateByDataProcessorId(tenantId, dataProcessorId);
 * </pre>
 *
 * @since 2.0.0
 */
@Slf4j
@Repository
public class BusinessKeyRepository {

    private static final String COLLECTION_SYSTEM_CONFIGURATIONS = "system_configurations";
    private static final String COLLECTION_DATA_PROCESSORS = "data_processors";
    private static final String FIELD_BUSINESS_ID = "businessId";
    private static final String FIELD_DATA_PROCESSOR_ID = "dataProcessorId";
    private static final String FIELD_CONFIGURATION_JSON = "configurationJson";
    private static final String FIELD_SSL_CONFIGURATIONS = "sslCertificate";
    private static final String FIELD_CERTIFICATE = "certificate";

    @Autowired
    private MongoTemplateProvider mongoTemplateProvider;

    /**
     * Finds the certificate for a DATA_FIDUCIARY by business ID.
     *
     * <p>This method retrieves the mutual SSL certificate from the notification_configurations
     * collection. The certificate is stored in the 'configurationJson.mutualCertificate' field
     * and is typically a Base64-encoded X.509 certificate.</p>
     *
     * <p><b>Certificate Field Path:</b></p>
     * <pre>
     * notification_configurations {
     *   businessId: "business123",
     *   configurationJson: {
     *     mutualCertificate: "base64,MIIDdzCCAl+gAwIBAgI..."
     *   }
     * }
     * </pre>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param businessId the business ID to search for
     * @return the Base64-encoded certificate string, or null if not found
     */
    public String findCertificateByBusinessId(String tenantId, String businessId) {
        try {
            MongoTemplate template = mongoTemplateProvider.getTemplate(tenantId);

            Query query = new Query(Criteria.where(FIELD_BUSINESS_ID).is(businessId));
            query.fields().include(FIELD_CONFIGURATION_JSON + "." + FIELD_SSL_CONFIGURATIONS);

            Document doc = template.findOne(query, Document.class, COLLECTION_SYSTEM_CONFIGURATIONS);
            if (doc == null) {
                log.warn("No notification configuration found for businessId={} in tenant={}",
                        businessId, tenantId);
                return null;
            }

            Object configObj = doc.get(FIELD_CONFIGURATION_JSON);
            if (!(configObj instanceof Document)) {
                log.warn("configurationJson not found or invalid for businessId={} in tenant={}",
                        businessId, tenantId);
                return null;
            }

            Document configJson = (Document) configObj;
            String certificate = configJson.getString(FIELD_SSL_CONFIGURATIONS);

            if (certificate == null || certificate.isBlank()) {
                log.warn("mutualCertificate not found or empty for businessId={} in tenant={}",
                        businessId, tenantId);
                return null;
            }

            log.debug("Successfully retrieved certificate for businessId={} in tenant={}",
                    businessId, tenantId);
            return certificate;

        } catch (Exception e) {
            log.error("Error retrieving certificate for businessId={} in tenant={}: {}",
                    businessId, tenantId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Finds the certificate for a DATA_PROCESSOR by data processor ID.
     *
     * <p>This method retrieves the certificate from the data_processors collection.
     * The certificate is stored in the 'attachment' field and is typically a Base64-encoded
     * X.509 certificate.</p>
     *
     * <p><b>Certificate Field Path:</b></p>
     * <pre>
     * data_processors {
     *   dataProcessorId: "dp123",
     *   businessId: "business123",
     *   attachment: "base64,MIIDdzCCAl+gAwIBAgI..."
     * }
     * </pre>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param dataProcessorId the data processor ID to search for
     * @return the Base64-encoded certificate string, or null if not found
     */
    public String findCertificateByDataProcessorId(String tenantId, String dataProcessorId) {
        try {
            MongoTemplate template = mongoTemplateProvider.getTemplate(tenantId);

            Query query = new Query(Criteria.where(FIELD_DATA_PROCESSOR_ID).is(dataProcessorId));
            query.fields().include(FIELD_CERTIFICATE);

            Document doc = template.findOne(query, Document.class, COLLECTION_DATA_PROCESSORS);
            if (doc == null) {
                log.warn("No data processor found for dataProcessorId={} in tenant={}",
                        dataProcessorId, tenantId);
                return null;
            }

            String certificate = doc.getString(FIELD_CERTIFICATE);
            if (certificate == null || certificate.isBlank()) {
                log.warn("Attachment (certificate) not found or empty for dataProcessorId={} in tenant={}",
                        dataProcessorId, tenantId);
                return null;
            }

            log.debug("Successfully retrieved certificate for dataProcessorId={} in tenant={}",
                    dataProcessorId, tenantId);
            return certificate;

        } catch (Exception e) {
            log.error("Error retrieving certificate for dataProcessorId={} in tenant={}: {}",
                    dataProcessorId, tenantId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validates if a certificate exists for a given business ID.
     *
     * <p>This method checks if a certificate exists without retrieving the full document,
     * which is more efficient for validation purposes.</p>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param businessId the business ID to check
     * @return true if certificate exists and is not empty, false otherwise
     */
    public boolean certificateExistsForBusinessId(String tenantId, String businessId) {
        String certificate = findCertificateByBusinessId(tenantId, businessId);
        return certificate != null && !certificate.isBlank();
    }

    /**
     * Validates if a certificate exists for a given data processor ID.
     *
     * <p>This method checks if a certificate exists without retrieving the full document,
     * which is more efficient for validation purposes.</p>
     *
     * @param tenantId the tenant ID for multi-tenancy isolation
     * @param dataProcessorId the data processor ID to check
     * @return true if certificate exists and is not empty, false otherwise
     */
    public boolean certificateExistsForDataProcessorId(String tenantId, String dataProcessorId) {
        String certificate = findCertificateByDataProcessorId(tenantId, dataProcessorId);
        return certificate != null && !certificate.isBlank();
    }
}
