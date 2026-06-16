package com.example.scanner.dto.request;

import com.example.scanner.dto.CustomerIdentifiers;
import com.example.scanner.enums.LANGUAGE;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggerEventRequest {

    private String eventType;
    private String resource;
    private CustomerIdentifiers customerIdentifiers;
    private List<String> dataProcessorIds;

    @Builder.Default
    private LANGUAGE language = LANGUAGE.ENGLISH;
    
    private Object eventPayload;

}
