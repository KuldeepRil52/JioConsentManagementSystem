package com.jio.vault.dto;

public class OnboardCertResponse {

    private String message;
    private String tenantId;
    private String businessId;
    private String publicKeyPem;
    private String certType;


    public OnboardCertResponse() {
    }

    public OnboardCertResponse(String tenantId, String businessId, String publicKeyPem, String certType, String message) {
        this.tenantId = tenantId;
        this.businessId = businessId;
        this.publicKeyPem = publicKeyPem;
        this.certType = certType;
        this.message = message;
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public String getCertType() {
        return certType;
    }



    // Setters
    public void setMessage(String message) {
        this.message = message;
    }
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public void setCertType(String certType) {
        this.certType = certType;
    }


}

