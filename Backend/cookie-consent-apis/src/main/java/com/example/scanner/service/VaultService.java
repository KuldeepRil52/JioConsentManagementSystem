package com.example.scanner.service;

import com.example.scanner.client.VaultClient;
import com.example.scanner.dto.response.EncryptPayloadResponse;
import com.example.scanner.dto.response.VaultVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VaultService {

    private final VaultClient vaultClient;

    /**
     * Sign any JSON string using vault API
     */
    public String signJsonPayload(String jsonPayload, String tenantId, String businessId) {
        log.info("Signing JSON payload via vault service");
        return vaultClient.signPayload(jsonPayload, tenantId, businessId);
    }

    /**
     * Verify JWT token using vault API
     */
    public VaultVerifyResponse verifyJwtToken(String jwt, String tenantId, String businessId) {
        log.info("Verifying JWT token via vault service");
        return vaultClient.verifyToken(jwt, tenantId, businessId);
    }


    public EncryptPayloadResponse encryptPayload(String tenantId, String businessId, String dataCategoryType, String dataCategoryValue, String dataString) {
        log.info("Encrypting payload via vault service");
        return vaultClient.encryptPayload(tenantId, businessId, dataCategoryType, dataCategoryValue, dataString);
    }
}