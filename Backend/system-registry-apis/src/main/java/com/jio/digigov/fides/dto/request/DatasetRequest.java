package com.jio.digigov.fides.dto.request;

import com.jio.digigov.fides.constant.ErrorCodes;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DatasetRequest {

    @NotBlank(message = ErrorCodes.JCMP1006)
    private String datasetYaml;
}

