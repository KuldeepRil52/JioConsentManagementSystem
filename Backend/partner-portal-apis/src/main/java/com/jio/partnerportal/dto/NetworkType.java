package com.jio.partnerportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Network type for notification service")
public enum NetworkType {
    @Schema(description = "RIL Network - uses bearer token only")
    INTRANET,
    
    @Schema(description = "External Network - requires mutual SSL + bearer token")
    INTERNET
}

