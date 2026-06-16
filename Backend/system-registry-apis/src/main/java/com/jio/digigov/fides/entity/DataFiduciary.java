package com.jio.digigov.fides.entity;

import com.jio.digigov.fides.enumeration.ExecutionStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class DataFiduciary {

    private List<DataFiduciarySystem> systems;
    private ExecutionStatus overallStatus;
    private Instant executedAt;
}