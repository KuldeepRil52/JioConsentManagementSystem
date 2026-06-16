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
import com.jio.partnerportal.dto.PremiumMeta;
import com.jio.partnerportal.dto.response.GetSubscriptionResponse;
import com.jio.partnerportal.dto.response.SubscriptionResponse;
import com.jio.partnerportal.entity.TenantRegistry;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.TenantRepository;
import com.jio.partnerportal.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SubscriptionService {

    private TenantRepository tenantRepository;
    private AuditManager auditManager;

    @Autowired
    public SubscriptionService(TenantRepository tenantRepository, AuditManager auditManager) {
        this.tenantRepository = tenantRepository;
        this.auditManager = auditManager;
    }

    public SubscriptionResponse enableSubscription(String tenantId, HttpServletRequest req) throws PartnerPortalException {
        String activity = "Enable premium subscription";

        TenantRegistry tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new PartnerPortalException(ErrorCodes.JCMP4006)); // Tenant not found

        if (tenant.isPremium()) {
            throw new PartnerPortalException(ErrorCodes.JCMP4007); // Subscription already active
        }

        // Create premium metadata for 3 months validity
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusMonths(3);
        String licenceId = generateLicenceId();

        PremiumMeta premiumMeta = PremiumMeta.builder()
                .startDate(startDate)
                .endDate(endDate)
                .licenceId(licenceId)
                .build();

        tenant.setPremium(true);
        tenant.setPremiumMeta(premiumMeta);

        tenantRepository.save(tenant);

        log.info("Premium subscription enabled for tenant: {} with licence: {}", tenantId, licenceId);
        LogUtil.logActivity(req, activity, "Success: Enable premium subscription successfully");
        this.logSubscriptionAudit(tenant, ActionType.ENABLE);
        return SubscriptionResponse.builder()
                .message("Subscription enabled successfully")
                .licenceId(licenceId)
                .premiumMeta(premiumMeta)
                .isPremium(true)
                .build();
    }

    public SubscriptionResponse renewSubscription(String tenantId,HttpServletRequest req) throws PartnerPortalException {
        String activity = "Renew premium subscription";

        TenantRegistry tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new PartnerPortalException(ErrorCodes.JCMP4006)); // Tenant not found

        if (!tenant.isPremium()) {
            throw new PartnerPortalException(ErrorCodes.JCMP4008); // No active subscription to renew
        }

        PremiumMeta currentMeta = tenant.getPremiumMeta();
        if (currentMeta == null) {
            throw new PartnerPortalException(ErrorCodes.JCMP4010); // Invalid subscription metadata
        }

        // Check if subscription is expired
        if (currentMeta.getEndDate().isBefore(LocalDateTime.now())) {
            // Renew for 3 months from current date
            LocalDateTime newStartDate = LocalDateTime.now();
            LocalDateTime newEndDate = newStartDate.plusMonths(3);
            String newLicenceId = generateLicenceId();

            PremiumMeta newPremiumMeta = PremiumMeta.builder()
                    .startDate(newStartDate)
                    .endDate(newEndDate)
                    .licenceId(newLicenceId)
                    .build();

            tenant.setPremiumMeta(newPremiumMeta);
            tenantRepository.save(tenant);

            log.info("Premium subscription renewed for tenant: {} with new licence: {}", tenantId, newLicenceId);
            LogUtil.logActivity(req, activity, "Success: Renew premium subscription successfully");
            this.logSubscriptionAudit(tenant, ActionType.RENEW);
            return SubscriptionResponse.builder()
                    .message("Subscription renewed successfully")
                    .licenceId(newLicenceId)
                    .premiumMeta(newPremiumMeta)
                    .isPremium(true)
                    .build();
        } else {
            throw new PartnerPortalException(ErrorCodes.JCMP4009); // Subscription not expired yet
        }
    }

    public GetSubscriptionResponse getSubscription(String tenantId, String pan, String clientId,HttpServletRequest req) throws PartnerPortalException {
        String activity = "Get subscription details";

        TenantRegistry tenant = null;

        if (!ObjectUtils.isEmpty(tenantId)) {
            tenant = tenantRepository.findByTenantId(tenantId).orElse(null);
        } else if (!ObjectUtils.isEmpty(pan)) {
            tenant = tenantRepository.findByPan(pan).orElse(null);
        } else if (!ObjectUtils.isEmpty(clientId)) {
            tenant = tenantRepository.findByClientId(clientId).orElse(null);
        }

        if (tenant == null) {
            throw new PartnerPortalException(ErrorCodes.JCMP4006); // Tenant not found
        }
        LogUtil.logActivity(req, activity, "Success: Get subscription details successfully");
        return GetSubscriptionResponse.builder()
                .isPremium(tenant.isPremium())
                .premiumMeta(tenant.getPremiumMeta())
                .tenantId(tenant.getTenantId())
                .pan(tenant.getPan())
                .clientId(tenant.getClientId())
                .build();
    }

    private String generateLicenceId() {
        return "LIC-" + LocalDateTime.now().getYear() + "-" + 
               String.format("%03d", LocalDateTime.now().getDayOfYear()) + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Modular function to log Subscription audit events
     * Can be used in both create and update Subscription flows
     *
     * @param tenantRegistry The tenantRegistry entity to audit
     * @param actionType The action type (ENABLE, RENEW)
     */
    public void logSubscriptionAudit(TenantRegistry tenantRegistry, ActionType actionType) {
        try {
            String tenantId = ThreadContext.get(Constants.TENANT_ID_HEADER);
            Actor actor = Actor.builder()
                    .id(ThreadContext.get(Constants.USER_ID_THREAD_CONTEXT))
                    .role(Constants.USER)
                    .type(Constants.USER_ID_TYPE)
                    .build();

            Resource resource = Resource.builder()
                    .type(Constants.LICENCE_ID)
                    .id(tenantRegistry.getPremiumMeta().getLicenceId())
                    .build();

            Context context = Context.builder()
                    .ipAddress(ThreadContext.get(Constants.SOURCE_IP) != null && !ThreadContext.get(Constants.SOURCE_IP).equals("-")
                            ? ThreadContext.get(Constants.SOURCE_IP) : null)
                    .txnId(ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) != null && !ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT).equals("-") 
                            ? ThreadContext.get(Constants.TXN_ID_THREAD_CONTEXT) : null)
                    .build();

            Map<String, Object> extra = new HashMap<>();
            // Add tenantRegistry POJO in the extra field under the "data" key
            extra.put(Constants.DATA, tenantRegistry);

            AuditRequest auditRequest = AuditRequest.builder()
                    .actor(actor)
                    .businessId(tenantId)
                    .group(Constants.PARTNER_PORTAL_GROUP)
                    .component(AuditComponent.SUBSCRIPTION)
                    .actionType(actionType)
                    .resource(resource)
                    .initiator(Constants.DATA_FIDUCIARY)
                    .context(context)
                    .extra(extra)
                    .build();

            this.auditManager.logAudit(auditRequest, tenantId);
        } catch (Exception e) {
            log.error("Audit logging failed for Subscription Licence id: {}, action: {}, error: {}", 
                    tenantRegistry.getPremiumMeta().getLicenceId(), actionType, e.getMessage(), e);
        }
    }
}
