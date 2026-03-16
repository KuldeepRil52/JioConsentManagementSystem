package com.jio.digigov.grievance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import com.jio.digigov.grievance.enumeration.GrievanceUpdateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(alphabetic=true)
public class GrievanceUpdateResponseDto {
    private String message;
    private String grievanceId;
    private String grievanceJwtToken;
    private GrievanceStatus status;
    private String transactionId;
    private LocalDateTime timestamp;
}
