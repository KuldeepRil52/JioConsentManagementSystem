package com.jio.digigov.fides.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "System count response")
public class SystemCountResponse {

    @Schema(example = "25")
    private int totalSystems;

    @Schema(example = "20")
    private int activeSystems;

    @Schema(example = "5")
    private int inactiveSystems;
}