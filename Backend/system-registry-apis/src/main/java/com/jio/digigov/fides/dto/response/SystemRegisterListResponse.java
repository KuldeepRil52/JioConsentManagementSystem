package com.jio.digigov.fides.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "List of systems response")
public class SystemRegisterListResponse {

    @Schema(example = "2")
    private int totalRecords;

    private List<SystemRegisterResponse> systems;
}