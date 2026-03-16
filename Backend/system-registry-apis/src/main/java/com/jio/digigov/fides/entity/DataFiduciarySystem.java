package com.jio.digigov.fides.entity;

import com.jio.digigov.fides.enumeration.ExecutionStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class DataFiduciarySystem {

    private String systemId;
    private String deletionType;
    private String yamlMappings;
    private ExecutionStatus status;
    private Instant executedAt;
    private String deferredReason;
}