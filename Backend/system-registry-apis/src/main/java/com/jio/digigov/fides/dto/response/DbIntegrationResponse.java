package com.jio.digigov.fides.dto.response;

import com.jio.digigov.fides.enumeration.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class DbIntegrationResponse {

    private String integrationId;
    private String systemId;
    private String businessId;
    private String dbType;
    private Map<String, Object> connectionDetails;
    private String datasetId;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}