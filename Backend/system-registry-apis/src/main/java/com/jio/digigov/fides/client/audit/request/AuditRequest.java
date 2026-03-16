package com.jio.digigov.fides.client.audit.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.digigov.fides.enumeration.ActionType;
import com.jio.digigov.fides.enumeration.AuditComponent;
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

