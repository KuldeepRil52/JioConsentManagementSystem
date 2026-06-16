package com.jio.consent.dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplateResponse {

    private String templateId;
    private String message;

}
