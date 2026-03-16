package com.jio.partnerportal.client.notification.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.partnerportal.constant.ErrorCodes;
import com.jio.partnerportal.dto.IdentityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for triggering notification events")
public class TriggerEventRequest {

    @Schema(description = "Type of event to trigger", example = "CONSENT_CREATED")
    @JsonProperty("eventType")
    private String eventType;

    @Schema(description = "Resource identifier", example = "consent")
    @JsonProperty("resource")
    private String resource;

    @Schema(description = "List of data processor IDs")
    @JsonProperty("dataProcessorIds")
    private List<String> dataProcessorIds;

    @Schema(description = "Customer identifiers")
    @JsonProperty("customerIdentifiers")
    private CustomerIdentifiers customerIdentifiers;

    @Schema(description = "Language preference", example = "en")
    @JsonProperty("language")
    private String language;

    @Schema(description = "Event payload data")
    @JsonProperty("eventPayload")
    private Object eventPayload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Customer identifiers")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerIdentifiers {

        @Schema(description = "Identity type", example = "MOBILE", allowableValues = {"MOBILE", "EMAIL"})
        @NotNull(message = ErrorCodes.JCMP1016)
        private IdentityType type;
        @NotBlank(message = ErrorCodes.JCMP1017)
        private String value;

        @Schema(hidden = true)
        @AssertTrue(message = ErrorCodes.JCMP1018)
        public boolean isValueValidForType() {
            if (type == null || value == null) {
                return true;
            }

            return switch (type) {
                case MOBILE -> value.matches("^\\d{10}$");
                case EMAIL -> value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
            };
        }
    }
}
