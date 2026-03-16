package com.jio.digigov.fides.entity;

import com.jio.digigov.fides.enumeration.Status;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Document(collection = "dataset_registry")
public class Dataset extends BaseEntity {

    @Id
    private String id; // Mongo ObjectId

    private String datasetId;      // Business-level unique ID
    private String businessId;

    private String datasetYaml;

    private Integer version;

    private Status status;

    private boolean isDeleted = false;
}

