package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.DPOConfig;

import java.util.List;
import java.util.Map;

public interface DPORepository {

    DPOConfig save(DPOConfig config);

    DPOConfig findByConfigId(String configId);

    List<DPOConfig> findConfigByParams(Map<String, String> searchParams);

    long count();

    boolean existByScopeLevel(String scopeLevel);

    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);

}
