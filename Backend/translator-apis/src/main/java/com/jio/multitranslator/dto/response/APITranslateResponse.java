package com.jio.multitranslator.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jio.multitranslator.dto.Status;
import com.jio.multitranslator.dto.response.translate.OutputItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing the API response for translation operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APITranslateResponse {
    
    @JsonProperty("TXN")
    private String txn;
    
    private Status status;
    private String message;
    private List<OutputItem> output;
}
