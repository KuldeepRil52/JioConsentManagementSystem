package com.jio.digigov.grievance.dto.request;

import com.jio.digigov.grievance.enumeration.GrievanceStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NotificationEventRequest {

    private String eventType;
    private String resource;
    private String source;
    private String grievanceId;
    private Map<String, Object> customerIdentifiers;
    private String language;
    private Map<String, Object> eventPayload;
}
