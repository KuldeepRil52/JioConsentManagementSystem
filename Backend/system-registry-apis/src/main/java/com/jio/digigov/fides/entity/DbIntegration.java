package com.jio.digigov.fides.entity;

import com.jio.digigov.fides.enumeration.Status;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Document(collection = "db_integration")
public class DbIntegration extends BaseEntity {

    @Id
    private String id;

    private String integrationId;
    private String systemId;
    private String businessId;
    private String dbType;

    private Map<String, Object> connectionDetails;

    private String datasetId;
    private Status status; // ACTIVE / INACTIVE

    private boolean isDeleted = false;
}