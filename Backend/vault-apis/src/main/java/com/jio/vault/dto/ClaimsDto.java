package com.jio.vault.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaimsDto {
    private String iss;        // issuer
    private String sub;        // subject
    private String aud;        // audience
    private Long exp;          // expiration time (epoch seconds)
    private Long nbf;          // not before
    private Long iat;          // issued at
    private String jti;        // JWT ID
    private String tenantId;
    private String businessId;
    private String keyId;

    public ClaimsDto() {}

    public ClaimsDto(String iss, String sub, String aud, Long exp, Long nbf,
                     Long iat, String jti, String tenantId, String businessId, String keyId) {
        this.iss = iss;
        this.sub = sub;
        this.aud = aud;
        this.exp = exp;
        this.nbf = nbf;
        this.iat = iat;
        this.jti = jti;
        this.tenantId = tenantId;
        this.businessId = businessId;
        this.keyId = keyId;
    }

    public String getIss() { return iss; }
    public void setIss(String iss) { this.iss = iss; }

    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }

    public String getAud() { return aud; }
    public void setAud(String aud) { this.aud = aud; }

    public Long getExp() { return exp; }
    public void setExp(Long exp) { this.exp = exp; }

    public Long getNbf() { return nbf; }
    public void setNbf(Long nbf) { this.nbf = nbf; }

    public Long getIat() { return iat; }
    public void setIat(Long iat) { this.iat = iat; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
}
