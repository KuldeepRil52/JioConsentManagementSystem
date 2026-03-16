package com.jio.partnerportal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PremiumMeta {

    @Schema(description = "Start date of the premium subscription", example = "2024-01-01T00:00:00")
    @JsonProperty("startDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @Schema(description = "End date of the premium subscription", example = "2024-04-01T00:00:00")
    @JsonProperty("endDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    @Schema(description = "Unique licence ID for the premium subscription", example = "LIC-2024-001-ABC123")
    @JsonProperty("licenceId")
    private String licenceId;
}
