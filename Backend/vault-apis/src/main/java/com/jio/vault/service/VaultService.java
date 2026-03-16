package com.jio.vault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.vault.client.VaultClient;
import com.jio.vault.constants.ErrorCode;
import com.jio.vault.dto.*;
import com.jio.vault.exception.CustomException;
import com.jio.vault.repository.ClientPublicCertRepositoryCustom;
import com.jio.vault.util.JwtGenerator;
import com.jio.vault.documents.ClientPublicCert;
import com.jio.vault.util.JwtVerifierUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
public class VaultService {

    //private final ClientPublicCertRepository repository;
    private final ClientPublicCertRepositoryCustom customRepo;
    private final VaultClient vaultClient;
    private final JwtGenerator jwtGenerator;

    public VaultService(VaultClient vaultClient, JwtGenerator jwtGenerator, ClientPublicCertRepositoryCustom customRepo) {
        //this.repository = repository;
        this.vaultClient = vaultClient;
        this.jwtGenerator = jwtGenerator;
        this.customRepo = customRepo;
    }


    public ClientPublicCert createAndSaveCert(String businessId, String tenantId, String certType) {
        certType = "rsa-4096";
        String dbName = "tenant_db_" + tenantId;
        UUID keyId = UUID.randomUUID();
        String keyIdStr = keyId.toString();
        String aesKeyIdStr = "aes-"+keyIdStr;
        VaultKeyResponse vaultResponse = vaultClient.createTransitKey(keyId.toString(), certType);
        VaultAesResponse aesResponse = vaultClient.createAesKey(keyId.toString());
        String publicKeyPem = vaultResponse.getData().getKeys().get("1").getPublicKey();
        ClientPublicCert cert = new ClientPublicCert(
                businessId,
                tenantId,
                keyIdStr,
                publicKeyPem,
                certType,
                aesKeyIdStr
        );
        return customRepo.save(dbName, cert);

    }

    public String generateJwtForTenantAndBusiness(String businessId, String tenantId, String payloadJson) {
        String dbName = "tenant_db_" + tenantId;
        ClientPublicCert cert = getClientPublicCert(dbName, tenantId, businessId);
        String keyId = cert.getKeyId();
        return jwtGenerator.generateJwt(payloadJson, keyId);
    }

    public VerifyJwtSignResponse verifyJwtForTenantAndBusiness(String tenantId, String businessId, String jwt) {
        JwtVerifierUtil verifierUtil = new JwtVerifierUtil(customRepo);
        return verifierUtil.verifyJwt(tenantId, businessId, jwt);
    }

    public ClientKeyDto getKeysByTenantAndBusiness(String tenantId, String businessId) {

        String dbName = "tenant_db_" + tenantId;
        ClientPublicCert cert = getClientPublicCert(dbName, tenantId, businessId);
        return new ClientKeyDto(cert.getKeyId(), cert.getPublicKeyPem());
    }

    public EncryptResponse encryptData(String tenantId, String businessId, String requestBody) {
        String dbName = "tenant_db_" + tenantId;
        ClientPublicCert cert = getClientPublicCert(dbName, tenantId, businessId);
        if (cert.getAesKey() == null || cert.getAesKey().isBlank()) {
            String aesKey = "aes-"+cert.getKeyId();
            VaultAesResponse aesResponse = vaultClient.createAesKey("aes-"+cert.getKeyId());
            cert.setAesKey(aesKey);
            customRepo.save(dbName, cert);
            log.info("Saved the AesResponse");
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            EncryptRequest req = mapper.readValue(requestBody, EncryptRequest.class);
            String base64Text = req.getBase64Text();

            String cipherText = vaultClient.encrypt(cert.getAesKey(), base64Text);
            log.info("Encrypted the cipherText");
            return new EncryptResponse(cert.getAesKey(), cipherText);
        } catch (Exception e) {
            log.error("Error while encrypting the cipherText {}", e.getMessage());
            throw new CustomException(ErrorCode.ENCRYPTION_FAILED, e.getMessage());
        }
    }

    public String encryptBase64Text(String tenantId, String businessId, String base64Text) {
        String dbName = "tenant_db_" + tenantId;
        ClientPublicCert cert = getClientPublicCert(dbName, tenantId, businessId);
        String aesKey = "aes-"+cert.getKeyId();
        if (cert.getAesKey() == null || cert.getAesKey().isBlank()) {
            log.info("Business does not have the AES key in the database");
            VaultAesResponse aesResponse = vaultClient.createAesKey(aesKey);
            log.info("AES key has been created");
            cert.setAesKey(aesKey);
            customRepo.save(dbName, cert);
        }
        try {
            String cipherText = vaultClient.encrypt(aesKey, base64Text);
            return cipherText;
        } catch (Exception e) {
            log.error("Error while encrypting the cipherText {}", e.getMessage());
            throw new CustomException(ErrorCode.ENCRYPTION_FAILED, e.getMessage());
        }
    }


    public DecryptResponse decryptData(String tenantId, String businessId, String requestBody) {
        String dbName = "tenant_db_" + tenantId;
        ClientPublicCert cert = getClientPublicCert(dbName, tenantId, businessId);
        try {
            ObjectMapper mapper = new ObjectMapper();
            DecryptRequest req = mapper.readValue(requestBody, DecryptRequest.class);
            String ciphertext = req.getCiphertext();
            String plaintext = vaultClient.decrypt(cert.getAesKey(), ciphertext);
            return new DecryptResponse(plaintext, cert.getKeyId());

        } catch (Exception e) {
            throw new CustomException(ErrorCode.DECRYPTION_FAILED, e.getMessage());
        }
    }

    public String decryptEncryptedPayload(String tenantId, String businessId, String ciphertext) {
        String dbName = "tenant_db_" + tenantId;
        ClientPublicCert cert = getClientPublicCert(dbName, tenantId, businessId);
        try {
            log.debug("decryptEncryptedPayload ciphertext: " + ciphertext+" with keyId: " + cert.getAesKey());
            String plaintext = vaultClient.decrypt(cert.getAesKey(), ciphertext);
            return plaintext;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.DECRYPTION_FAILED, e.getMessage());
        }
    }

    public ClientPublicCert  getClientPublicCert(String dbName, String tenantId, String businessId) {
        ClientPublicCert cert =
                customRepo.findByBusinessIdAndTenantIdDynamic(dbName, businessId, tenantId)
                        .orElseThrow(() -> {

                            log.error("No certificate exists for tenantId={} and businessId={}", tenantId, businessId);

                            return new CustomException(
                                    ErrorCode.INVALID_REQUEST,
                                    "No certificate exists for tenantId=" + tenantId + " and businessId=" + businessId
                            );
                        });
        return cert;
    }

    public String checkHealth(){
        return vaultClient.checkVaultHealth();
    }
}


