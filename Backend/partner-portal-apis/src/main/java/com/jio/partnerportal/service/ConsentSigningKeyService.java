package com.jio.partnerportal.service;

import com.jio.partnerportal.client.audit.AuditManager;
import com.jio.partnerportal.client.audit.request.Actor;
import com.jio.partnerportal.client.audit.request.AuditRequest;
import com.jio.partnerportal.client.audit.request.Context;
import com.jio.partnerportal.client.audit.request.Resource;
import com.jio.partnerportal.client.vault.VaultManager;
import com.jio.partnerportal.client.vault.request.OnboardKeyRequest;
import com.jio.partnerportal.client.vault.response.OnboardKeyResponse;
import com.jio.partnerportal.constant.Constants;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.ActionType;
import com.jio.partnerportal.dto.AuditComponent;
import com.jio.partnerportal.dto.response.ConsentSigningKeyResponse;
import com.jio.partnerportal.entity.BusinessApplication;
import com.jio.partnerportal.entity.ClientCredentials;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.BusinessApplicationRepository;
import com.jio.partnerportal.repository.ClientCredentialsRepository;
import com.jio.partnerportal.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ConsentSigningKeyService {

    private final ClientCredentialsRepository clientCredentialsRepository;
    private final VaultManager vaultManager;
    private final BusinessApplicationRepository businessApplicationRepository;
    private final AuditManager auditManager;

    @Autowired
    public ConsentSigningKeyService(ClientCredentialsRepository clientCredentialsRepository,
                                    VaultManager vaultManager,
                                    BusinessApplicationRepository businessApplicationRepository,
                                    AuditManager auditManager) {
        this.clientCredentialsRepository = clientCredentialsRepository;
        this.vaultManager = vaultManager;
        this.businessApplicationRepository = businessApplicationRepository;
        this.auditManager = auditManager;
    }

    public ConsentSigningKeyResponse generateConsentSigningKey(String tenantId, String businessId,
                                                               String scopeLevel, String certType, HttpServletRequest req)
            throws PartnerPortalException {

        String activity = "Generate consent signing key";

        boolean isExist = this.businessApplicationRepository.existByScopeLevelAndBusinessId(scopeLevel,businessId);
        if (!isExist) {
            throw  new PartnerPortalException(ErrorCodes.JCMP1004);
        }
        // Check if an entry with businessId and scopeLevel already exists
        ClientCredentials existingCredentials = clientCredentialsRepository.findByBusinessIdAndScopeLevel(businessId, scopeLevel);

        if (!ObjectUtils.isEmpty(existingCredentials) &&
                !ObjectUtils.isEmpty(existingCredentials.getPublicCertificate()) &&
                !ObjectUtils.isEmpty(existingCredentials.getCertType())) {

            log.info("Consent signing key already exists for businessId: {} and scopeLevel: {}", businessId, scopeLevel);

            // Return the existing value
            return ConsentSigningKeyResponse.builder()
                    .message("Consent signing key already exists")
                    .businessId(businessId)
                    .publicKeyPem(existingCredentials.getPublicCertificate())
                    .certType(existingCredentials.getCertType().toString())
                    .scopeLevel(scopeLevel)
                    .build();
        }

        // If not exists, call the Vault API
        OnboardKeyRequest onboardKeyRequest = OnboardKeyRequest.builder()
                .certType(certType)
                .build();

        OnboardKeyResponse onboardKeyResponse = vaultManager.onboardKey(tenantId, businessId, onboardKeyRequest);

        if (onboardKeyResponse == null || ObjectUtils.isEmpty(onboardKeyResponse.getPublicKeyPem())) {
            log.error("Failed to generate consent signing key from Vault for businessId: {}", businessId);
            throw new PartnerPortalException(ErrorCodes.JCMP5001);
        }

        // Update or create the client credentials with the public key
        ClientCredentials clientCredentials;
        if (existingCredentials != null) {
            // Update existing credentials
            existingCredentials.setPublicCertificate(onboardKeyResponse.getPublicKeyPem());
            existingCredentials.setCertType(onboardKeyResponse.getCertType());
            clientCredentials = existingCredentials;
        } else {
            // Create new credentials
            clientCredentials = ClientCredentials.builder()
                    .businessId(businessId)
                    .tenantId(tenantId)
                    .scopeLevel(scopeLevel)
                    .publicCertificate(onboardKeyResponse.getPublicKeyPem())
                    .certType(onboardKeyResponse.getCertType())
                    .status("ACTIVE")
                    .build();
        }

        // Save the credentials
        clientCredentialsRepository.save(clientCredentials);

        log.info("Consent signing key generated and saved successfully for businessId: {}", businessId);

        this.logConsentSigningKeyAudit(clientCredentials, ActionType.GENERATE);
        LogUtil.logActivity(req, activity, "Success: Generate consent signing key successfully");
        return ConsentSigningKeyResponse.builder()
                .message("Consent signing key generated successfully")
                .businessId(businessId)
                .publicKeyPem(onboardKeyResponse.getPublicKeyPem())
                .certType(onboardKeyResponse.getCertType().toString())
                .scopeLevel(scopeLevel)
                .build();
    }

    /**
     * Modular function to log consent signing key audit events
     * Can be used in both create and update consent signing key flows
     *
     * @param clientCredentials The client credentials entity to audit
     * @param actionType The action type (GENERATE)
     */
    public void logConsentSigningKeyAudit(ClientCredentials clientCredentials, ActionType actionType) {
        try {
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.BUSINESS_UNIQUE_ID)
                    .id(clientCredentials.getBusinessUniqueId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-") 
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add client credential POJO in the extra field under the "data" key
            extra.put(Constants.DATA, clientCredentials);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(clientCredentials.getBusinessId())
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.CONSENT_SIGNING_KEY)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for business unique id: {}, action: {}, error: {}", 
                    clientCredentials.getBusinessUniqueId(), actionType, e.getMessage(), e);
        }
    }
}

