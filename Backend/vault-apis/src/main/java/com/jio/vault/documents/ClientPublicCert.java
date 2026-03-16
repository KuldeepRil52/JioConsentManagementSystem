package com.jio.vault.documents;

import com.jio.vault.constants.CollectionConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.UUID;


@Document(collection = CollectionConstants.CLIENT_PUBLIC_CERT)
public class ClientPublicCert {

        @Id
        private String id;

        private String businessId;
        private String tenantId;
        private String keyId;
        private String publicKeyPem;
        private String certType;
        private String aesKey;// new field

        public ClientPublicCert() {}

        public ClientPublicCert(String businessId, String tenantId, String keyId, String publicKeyPem, String certType, String aesKey) {
            this.businessId = businessId;
            this.tenantId = tenantId;
            this.keyId = keyId;
            this.publicKeyPem = publicKeyPem;
            this.certType = certType;
            this.aesKey = aesKey;
        }

        // Getters
        public String getId() { return id; }
        public String getBusinessId() { return businessId; }
        public String getTenantId() { return tenantId; }
        public String getKeyId() { return keyId; }
        public String getPublicKeyPem() { return publicKeyPem; }
        public String getCertType() { return certType; }
        public String getAesKey() { return aesKey; }

        // Setters
        public void setId(String id) {
            this.id = id;
        }

        public void setBusinessId(String businessId) {
            this.businessId = businessId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public void setPublicKeyPem(String publicKeyPem) {
            this.publicKeyPem = publicKeyPem;
        }

        public void setCertType(String certType) {
            this.certType = certType;
        }

        public void setAesKey(String aesKey) { this.aesKey = aesKey; }


}


