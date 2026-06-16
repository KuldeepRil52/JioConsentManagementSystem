package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.ConsentConfig;

import java.util.List;
import java.util.Map;

public interface ConsentRepository {

    ConsentConfig save(ConsentConfig config);

    ConsentConfig findByConfigId(String configId);

    List<ConsentConfig> findConfigByParams(Map<String, String> searchParams);

    long count();

    boolean existByScopeLevel(String scopeLevel);

    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);
}
