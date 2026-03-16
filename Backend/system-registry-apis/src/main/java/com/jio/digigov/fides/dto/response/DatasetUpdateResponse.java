package com.jio.digigov.fides.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Updated datasets response")
public class DatasetUpdateResponse {

    private String message;

    private DatasetResponse data;
}