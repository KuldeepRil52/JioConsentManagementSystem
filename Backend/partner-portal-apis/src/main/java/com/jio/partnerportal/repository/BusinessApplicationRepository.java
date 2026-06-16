package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.BusinessApplication;

import java.util.List;
import java.util.Map;

public interface BusinessApplicationRepository {

    BusinessApplication findBusinessApplicationByApplicationId(String businessApplicationId);

    BusinessApplication findByBusinessId(String businessId);

    List<BusinessApplication> findByBusinessIds(List<String> businessIds);

    BusinessApplication save(BusinessApplication businessApplication);

    List<BusinessApplication> findBusinessApplicationByParams(Map<String, String> param);

    long count();

    boolean existByScopeLevel(String scopeLevel);

    boolean existByScopeLevelAndBusinessId(String scopeLevel, String businessId);

    BusinessApplication findByName(String name);

    BusinessApplication findByNameExcludingBusinessId(String name, String excludeBusinessId);
}
