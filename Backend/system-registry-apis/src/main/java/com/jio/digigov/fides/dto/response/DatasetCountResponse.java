package com.jio.digigov.fides.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Dataset count response")
public class DatasetCountResponse {

    @Schema(example = "25")
    private int totalDatasets;

    @Schema(example = "20")
    private int activeDatasets;

    @Schema(example = "5")
    private int inactiveDatasets;
}