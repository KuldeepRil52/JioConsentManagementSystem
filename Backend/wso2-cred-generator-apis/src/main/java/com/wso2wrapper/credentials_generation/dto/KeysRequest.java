package com.wso2wrapper.credentials_generation.dto;

import java.util.List;
import java.util.Map;

public class KeysRequest {
        private String keyType;
        private String keyManager;
        private List<String> grantTypesToBeSupported;
        private String callbackUrl;
        private List<String> scopes;
        private int validityTime;
        private Map<String, Object> additionalProperties;

        // Getters and Setters
        public String getKeyType() {
            return keyType;
        }

        public void setKeyType(String keyType) {
            this.keyType = keyType;
        }

        public String getKeyManager() {
            return keyManager;
        }

        public void setKeyManager(String keyManager) {
            this.keyManager = keyManager;
        }

        public List<String> getGrantTypesToBeSupported() {
            return grantTypesToBeSupported;
        }

        public void setGrantTypesToBeSupported(List<String> grantTypesToBeSupported) {
            this.grantTypesToBeSupported = grantTypesToBeSupported;
        }

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public int getValidityTime() {
            return validityTime;
        }

        public void setValidityTime(int validityTime) {
            this.validityTime = validityTime;
        }

        public Map<String, Object> getAdditionalProperties() {
            return additionalProperties;
        }

        public void setAdditionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }
    }



