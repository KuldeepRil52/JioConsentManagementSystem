package com.jio.vault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class VaultAesResponse {

    private String request_id;
    private String lease_id;
    private boolean renewable;
    private long lease_duration;
    private Data data;
    private Object wrap_info;
    private Object warnings;
    private Object auth;
    private String mount_type;

    public String getRequest_id() { return request_id; }
    public void setRequest_id(String request_id) { this.request_id = request_id; }

    public String getLease_id() { return lease_id; }
    public void setLease_id(String lease_id) { this.lease_id = lease_id; }

    public boolean isRenewable() { return renewable; }
    public void setRenewable(boolean renewable) { this.renewable = renewable; }

    public long getLease_duration() { return lease_duration; }
    public void setLease_duration(long lease_duration) { this.lease_duration = lease_duration; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public Object getWrap_info() { return wrap_info; }
    public void setWrap_info(Object wrap_info) { this.wrap_info = wrap_info; }

    public Object getWarnings() { return warnings; }
    public void setWarnings(Object warnings) { this.warnings = warnings; }

    public Object getAuth() { return auth; }
    public void setAuth(Object auth) { this.auth = auth; }

    public String getMount_type() { return mount_type; }
    public void setMount_type(String mount_type) { this.mount_type = mount_type; }

    public static class Data {

        @JsonProperty("allow_plaintext_backup")
        private boolean allowPlaintextBackup;

        @JsonProperty("auto_rotate_period")
        private int autoRotatePeriod;

        @JsonProperty("deletion_allowed")
        private boolean deletionAllowed;

        private boolean derived;
        private boolean exportable;

        @JsonProperty("imported_key")
        private boolean importedKey;

        private Map<String, Object> keys;

        @JsonProperty("latest_version")
        private int latestVersion;

        @JsonProperty("min_available_version")
        private int minAvailableVersion;

        @JsonProperty("min_decryption_version")
        private int minDecryptionVersion;

        @JsonProperty("min_encryption_version")
        private int minEncryptionVersion;

        private String name;
        private boolean supports_decryption;
        private boolean supports_derivation;
        private boolean supports_encryption;
        private boolean supports_signing;
        private String type;

        // Getters and Setters
        public boolean isAllowPlaintextBackup() { return allowPlaintextBackup; }
        public void setAllowPlaintextBackup(boolean allowPlaintextBackup) { this.allowPlaintextBackup = allowPlaintextBackup; }

        public int getAutoRotatePeriod() { return autoRotatePeriod; }
        public void setAutoRotatePeriod(int autoRotatePeriod) { this.autoRotatePeriod = autoRotatePeriod; }

        public boolean isDeletionAllowed() { return deletionAllowed; }
        public void setDeletionAllowed(boolean deletionAllowed) { this.deletionAllowed = deletionAllowed; }

        public boolean isDerived() { return derived; }
        public void setDerived(boolean derived) { this.derived = derived; }

        public boolean isExportable() { return exportable; }
        public void setExportable(boolean exportable) { this.exportable = exportable; }

        public boolean isImportedKey() { return importedKey; }
        public void setImportedKey(boolean importedKey) { this.importedKey = importedKey; }

        public Map<String, Object> getKeys() { return keys; }
        public void setKeys(Map<String, Object> keys) { this.keys = keys; }

        public int getLatestVersion() { return latestVersion; }
        public void setLatestVersion(int latestVersion) { this.latestVersion = latestVersion; }

        public int getMinAvailableVersion() { return minAvailableVersion; }
        public void setMinAvailableVersion(int minAvailableVersion) { this.minAvailableVersion = minAvailableVersion; }

        public int getMinDecryptionVersion() { return minDecryptionVersion; }
        public void setMinDecryptionVersion(int minDecryptionVersion) { this.minDecryptionVersion = minDecryptionVersion; }

        public int getMinEncryptionVersion() { return minEncryptionVersion; }
        public void setMinEncryptionVersion(int minEncryptionVersion) { this.minEncryptionVersion = minEncryptionVersion; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isSupports_decryption() { return supports_decryption; }
        public void setSupports_decryption(boolean supports_decryption) { this.supports_decryption = supports_decryption; }

        public boolean isSupports_derivation() { return supports_derivation; }
        public void setSupports_derivation(boolean supports_derivation) { this.supports_derivation = supports_derivation; }

        public boolean isSupports_encryption() { return supports_encryption; }
        public void setSupports_encryption(boolean supports_encryption) { this.supports_encryption = supports_encryption; }

        public boolean isSupports_signing() { return supports_signing; }
        public void setSupports_signing(boolean supports_signing) { this.supports_signing = supports_signing; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
