package com.jio.partnerportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public enum IntakeMethods {
    @Schema(description = "Grievances submitted via web form")
    WEB_FORM,
    @Schema(description = "Grievances submitted via mobile application")
    MOBILE_APP,
    @Schema(description = "Grievances submitted via email")
    EMAIL,
    @Schema(description = "Grievances submitted via IVR system")
    IVR,
    @Schema(description = "Grievances submitted via walk-in")
    WALK_IN;
}
