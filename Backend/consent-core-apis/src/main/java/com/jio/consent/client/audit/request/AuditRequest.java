package com.jio.consent.client.audit.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.consent.dto.ActionType;
import com.jio.consent.dto.AuditComponent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditRequest {
    private Actor actor;
    private String businessId;
    private String group;
    private AuditComponent component;
    private ActionType actionType;
    private Resource resource;
    private String initiator;
    private Context context;
    private Map<String, Object> extra;
}

