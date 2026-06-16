package com.jio.vault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.vault.constants.ErrorCode;
import com.jio.vault.dto.cryptodto.EncryptPayloadResponse;
import com.jio.vault.dto.cryptodto.DecryptedPayloadResponse;

import com.jio.vault.constants.HeaderConstants;
import com.jio.vault.documents.EncryptedPayload;
import com.jio.vault.exception.CustomException;
import com.jio.vault.repository.EncryptedPayloadRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PayloadCryptoService {

    @Autowired
    private VaultService vaultService;

    @Autowired
    private TenantValidationService tenantValidationService;

    @Autowired
    private EncryptedPayloadRepositoryCustom encryptedPayloadRepository;

    public PayloadCryptoService() {}

    public EncryptPayloadResponse encryptAndStorePayload(Map<String, String> headers, String jsonPayload) {

        String base64Text = Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
        String tenantId = headers.get(HeaderConstants.TENANT_ID);
        String businessId = headers.get(HeaderConstants.BUSINESS_ID);
        String dataCategoryType = headers.get(HeaderConstants.DATA_CATEGORY_TYPE);
        String dataCategoryValue = headers.get(HeaderConstants.DATA_CATEGORY_VALUE);
        String uuid = UUID.randomUUID().toString();

        String dbName = "tenant_db_" + tenantId;

        String encryptedPayload = vaultService.encryptBase64Text(tenantId, businessId, base64Text);
        EncryptedPayload encryptedPayloadObject = new EncryptedPayload();
        encryptedPayloadObject.setUuid(uuid);
        encryptedPayloadObject.setTenantId(tenantId);
        encryptedPayloadObject.setBusinessId(businessId);
        encryptedPayloadObject.setDataCategoryType(dataCategoryType);
        encryptedPayloadObject.setDataCategoryValue(dataCategoryValue);
        encryptedPayloadObject.setEncryptedString(encryptedPayload);
        encryptedPayloadObject.setCreatedTimeStamp(LocalDateTime.now());

        encryptedPayloadRepository.save(dbName,encryptedPayloadObject);

        EncryptPayloadResponse response = new EncryptPayloadResponse();

        response.setUuid(uuid);
        response.setTenantId(tenantId);
        response.setBusinessId(businessId);
        response.setEncryptedString(encryptedPayload);
        response.setDataCategoryType(dataCategoryType);
        response.setDataCategoryValue(dataCategoryValue);
        response.setCreatedTimeStamp(encryptedPayloadObject.getCreatedTimeStamp().toString());

        return response;

    }

    public DecryptedPayloadResponse decryptCipher(Map<String, String> headers) {
        String tenantId = headers.get(HeaderConstants.TENANT_ID);
        String businessId = headers.get(HeaderConstants.BUSINESS_ID);
        String uuid = headers.get(HeaderConstants.UUID);

        String dbName = "tenant_db_" + tenantId;

        EncryptedPayload payload = encryptedPayloadRepository.findByUuid(dbName, uuid)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_ERROR, "Payload not found for ReferenceId: " + uuid));

        log.info(uuid + " is being decrypted");

        // Decrypt from Vault
        String decryptedBase64Payload = vaultService.decryptEncryptedPayload(tenantId, businessId, payload.getEncryptedString());

        // Decode Base64 to JSON string
        byte[] decodedBytes = Base64.getDecoder().decode(decryptedBase64Payload);
        String decodedJson = new String(decodedBytes, StandardCharsets.UTF_8);

        // Convert JSON string to object
        ObjectMapper mapper = new ObjectMapper();
        Object decryptedPayload;
        try {
            decryptedPayload = mapper.readValue(decodedJson, Object.class);
        } catch (Exception e) {
            log.error("Failed to parse decrypted payload" + e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "Failed to parse decrypted payload" + e.getMessage());
        }

        DecryptedPayloadResponse response = new DecryptedPayloadResponse();
        response.setUuid(uuid);
        response.setDecryptedPayload(decryptedPayload);

        return response;
    }

}
