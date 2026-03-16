package com.jio.digigov.fides.client.notification.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.fides.dto.CustomerIdentifiers;
import com.jio.digigov.fides.enumeration.LANGUAGE;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MissedNotificationRequest {

    private String status;
    private String acknowledgementId;
    private String remark;
}