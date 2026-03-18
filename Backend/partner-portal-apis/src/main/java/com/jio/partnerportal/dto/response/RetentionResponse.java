package com.jio.partnerportal.dto.response;

import com.jio.partnerportal.entity.RetentionConfig;
import lombok.Data;

@Data
public class RetentionResponse {

    private String businessId;
    private String retentionId;
    private RetentionConfig.Retentions retentions;
    private String createdAt;
    private String updatedAt;
}
