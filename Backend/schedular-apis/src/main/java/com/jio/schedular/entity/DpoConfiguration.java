package com.jio.schedular.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "dpo_configurations")
@Data
public class DpoConfiguration {
    private String configId;
    private String businessId;
    private String scopeLevel;
    private Map<String, Object> configurationJson;
    private boolean isActive;
}