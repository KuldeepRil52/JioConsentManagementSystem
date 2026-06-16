package com.jio.consent.client.notification.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.CustomerIdentifiers;
import com.jio.consent.dto.LANGUAGE;
import com.jio.consent.dto.NotificationEvent;
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
public class TriggerEventRequest {

    private String eventType;
    private String resource;
    private CustomerIdentifiers customerIdentifiers;
    private List<String> dataProcessorIds;
    private LANGUAGE language = LANGUAGE.ENGLISH;
    private Object eventPayload;

}
