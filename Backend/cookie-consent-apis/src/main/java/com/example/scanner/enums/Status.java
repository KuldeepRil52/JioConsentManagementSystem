package com.example.scanner.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of consent")
public enum Status {
    @Schema(description = "Consent is currently active")
    ACTIVE,

    @Schema(description = "Consent has been revoked by user")
    REVOKED,

    @Schema(description = "Consent has expired")
    EXPIRED
}