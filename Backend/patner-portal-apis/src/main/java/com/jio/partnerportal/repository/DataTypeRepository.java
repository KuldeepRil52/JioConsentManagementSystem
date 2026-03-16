package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.DataType;

import java.util.List;
import java.util.Map;

public interface DataTypeRepository {

    DataType save(DataType purpose);

    DataType findByDataTypeId(String dataTypeId);

    List<DataType> findDataTypeByParams(Map<String, String> searchParams);

    long count();

    boolean existsByDataTypeName(String dataTypeName);

    boolean existsByDataTypeNameExcludingDataTypeId(String dataTypeName, String excludeDataTypeId);

}
