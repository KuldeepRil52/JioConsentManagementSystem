package com.jio.digigov.fides.dto.response;

import com.jio.digigov.fides.enumeration.Status;
import com.jio.digigov.fides.entity.Dataset;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class DatasetResponse {

    private String datasetId;
    private String businessId;
    private String datasetYaml;
    private Status status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DatasetResponse from(Dataset d) {
        return DatasetResponse.builder()
                .datasetId(d.getDatasetId())
                .businessId(d.getBusinessId())
                .datasetYaml(d.getDatasetYaml())
                .status(d.getStatus())
                .version(d.getVersion())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
