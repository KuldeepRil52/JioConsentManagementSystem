package com.jio.digigov.auditmodule.client.notification.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.auditmodule.dto.CustomerIdentifiers;
import com.jio.digigov.auditmodule.enumeration.LANGUAGE;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggerEventRequest {

    private String eventType;
    private String resource;
    private Map<String, Object> customerIdentifiers;
    private LANGUAGE language = LANGUAGE.ENGLISH;
    private Object eventPayload;

}
