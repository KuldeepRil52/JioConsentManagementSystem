package com.jio.auth.dto;

import lombok.Data;

@Data
public class TenantSecretCode {
    private String identityValue;
    private boolean active;

}
