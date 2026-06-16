package com.jio.vault.dto;

public class VerifySignResponse {
    private Data data;

    public VerifySignResponse(boolean valid) {
        this.data = new Data(valid);
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private boolean valid;

        public Data(boolean valid) {
            this.valid = valid;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}

