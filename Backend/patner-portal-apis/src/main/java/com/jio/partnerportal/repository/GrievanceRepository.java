package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.GrievanceConfig;

import java.util.List;
import java.util.Map;

public interface GrievanceRepository {

    GrievanceConfig save(GrievanceConfig config);

    GrievanceConfig findByConfigId(String configId);

    List<GrievanceConfig> findConfigByParams(Map<String, String> searchParams);

    long count();

    boolean existByScopeLevel(String scopeLevel);

    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);

}
