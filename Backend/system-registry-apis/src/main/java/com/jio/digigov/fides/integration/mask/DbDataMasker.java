package com.jio.digigov.fides.integration.mask;

import com.jio.digigov.fides.dto.CustomerIdentifiers;

import java.util.Set;
import java.util.Map;

public interface DbDataMasker {

    void maskData(
            Map<String, Object> connectionDetails,
            String datasetYaml,
            Set<String> dataItems,
            CustomerIdentifiers customerIdentifiers
    );
}
