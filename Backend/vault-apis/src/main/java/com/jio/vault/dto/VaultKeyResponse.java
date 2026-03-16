package com.jio.vault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class VaultKeyResponse {

    private Data data;

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public static class Data {
        private String name;
        private String type;
        private Map<String, KeyInfo> keys;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Map<String, KeyInfo> getKeys() { return keys; }
        public void setKeys(Map<String, KeyInfo> keys) { this.keys = keys; }

        public static class KeyInfo {
            @JsonProperty("public_key")
            private String publicKey;

            public String getPublicKey() { return publicKey; }
            public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
        }
    }
}

