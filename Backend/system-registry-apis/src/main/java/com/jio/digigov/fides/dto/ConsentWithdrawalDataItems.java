package com.jio.digigov.fides.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
public class ConsentWithdrawalDataItems {
    private Set<String> deletableItems; // pseudonymized
    // dataItem -> consentIds causing deferral
    private Map<String, Set<String>> deferredItems;
}