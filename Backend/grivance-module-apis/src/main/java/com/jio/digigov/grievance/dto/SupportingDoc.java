package com.jio.digigov.grievance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a supporting document attached to a grievance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportingDoc {
    private String docName;
    private String doc;      // base64 string
}
