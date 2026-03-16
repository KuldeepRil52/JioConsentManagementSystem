package com.jio.partnerportal.service;

import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.response.ClientCredentialsResponse;
import com.jio.partnerportal.entity.ClientCredentials;
import com.jio.partnerportal.exception.PartnerPortalException;
import com.jio.partnerportal.repository.ClientCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
public class ClientCredentialsService {

    private final ClientCredentialsRepository clientCredentialsRepository;

    @Autowired
    public ClientCredentialsService(ClientCredentialsRepository clientCredentialsRepository) {
        this.clientCredentialsRepository = clientCredentialsRepository;
    }

    /**
     * Fetches client credentials by businessId
     * The tenant-id from headers is used to select the appropriate database
     * 
     * @param businessId The business ID to search for
     * @return ClientCredentialsResponse object containing consumer key and secret (excluding id, publicCertificate, certType)
     * @throws PartnerPortalException if credentials are not found
     */
    public ClientCredentialsResponse getClientCredentialsByBusinessId(String businessId) throws PartnerPortalException {
        log.info("Fetching client credentials for businessId: {}", businessId);
        
        ClientCredentials clientCredentials = this.clientCredentialsRepository.findByBusinessId(businessId);
        
        if (ObjectUtils.isEmpty(clientCredentials)) {
            log.warn("Client credentials not found for businessId: {}", businessId);
            throw new PartnerPortalException(ErrorCodes.JCMP3001);
        }
        
        log.info("Successfully retrieved client credentials for businessId: {}", businessId);
        
        // Map to response DTO, excluding id, publicCertificate, and certType
        return ClientCredentialsResponse.builder()
                .businessId(clientCredentials.getBusinessId())
                .businessUniqueId(clientCredentials.getBusinessUniqueId())
                .consumerKey(clientCredentials.getConsumerKey())
                .consumerSecret(clientCredentials.getConsumerSecret())
                .scopeLevel(clientCredentials.getScopeLevel())
                .status(clientCredentials.getStatus())
                .tenantId(clientCredentials.getTenantId())
                .createdAt(clientCredentials.getCreatedAt())
                .updatedAt(clientCredentials.getUpdatedAt())
                .build();
    }
}

