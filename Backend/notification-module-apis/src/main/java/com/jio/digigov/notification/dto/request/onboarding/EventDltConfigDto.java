package com.jio.digigov.notification.dto.request.onboarding;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDltConfigDto {

    @NotBlank(message = "DLT Entity ID is required")
    private String dltEntityId;

    @NotBlank(message = "DLT Template ID is required")
    private String dltTemplateId;
}
