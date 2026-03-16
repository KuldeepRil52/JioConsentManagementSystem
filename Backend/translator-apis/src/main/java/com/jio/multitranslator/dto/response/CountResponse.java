package com.jio.multitranslator.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.multitranslator.dto.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the response for translation configuration count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CountResponse {
    private Status status;
    private long count;
    private String message;
}
