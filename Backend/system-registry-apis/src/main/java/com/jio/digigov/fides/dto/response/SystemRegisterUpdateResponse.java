package com.jio.digigov.fides.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "Updated systems response")
public class SystemRegisterUpdateResponse {

    private String message;

    private SystemRegisterResponse data;
}