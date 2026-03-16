package com.example.scanner.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of a preference choice")
public enum PreferenceStatus {
    @Schema(description = "User has accepted this preference")
    ACCEPTED,

    @Schema(description = "User has not accepted this preference")
    NOTACCEPTED,

    @Schema(description = "Preference has expired")
    EXPIRED
}