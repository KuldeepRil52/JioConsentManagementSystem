package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.DataProcessor;

import java.util.List;
import java.util.Map;

public interface DataProcessorRepository {
    DataProcessor save(DataProcessor dataProcessor);

    DataProcessor findByDataProcessorId(String dataProcessorId);

    List<DataProcessor> findDataProcessorByParams(Map<String, String> searchParams);

    long count();

    boolean existsByDataProcessorName(String dataProcessorName);

    boolean existsByDataProcessorNameExcludingDataProcessorId(String dataProcessorName, String excludeDataProcessorId);
}
