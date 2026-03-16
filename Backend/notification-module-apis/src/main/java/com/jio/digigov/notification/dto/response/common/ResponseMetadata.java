package com.jio.digigov.notification.dto.response.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response metadata")
public class ResponseMetadata {
    
    @Schema(description = "Response timestamp", example = "2024-01-20T10:30:45.123Z")
    private String timestamp;
    
    @Schema(description = "Transaction ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String transactionId;
}