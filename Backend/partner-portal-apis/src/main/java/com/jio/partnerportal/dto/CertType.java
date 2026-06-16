package com.jio.partnerportal.dto;

public enum CertType {
    RSA_2048,
    RSA_3072,
    RSA_4096;

    public String getValue() {
        return this.name().toLowerCase().replace("_", "-");
    }
}

