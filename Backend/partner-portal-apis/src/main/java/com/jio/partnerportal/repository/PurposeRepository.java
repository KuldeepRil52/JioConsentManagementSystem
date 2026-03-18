package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.Purpose;

import java.util.List;
import java.util.Map;

public interface PurposeRepository {

    Purpose save(Purpose purpose);

    Purpose findByPurposeId(String purposeId);

    Purpose findByPurposeCode(String purposeCode);

    List<Purpose> findByPurposeIds(List<String> purposeIds);

    List<Purpose> findPurposeByParams(Map<String, String> searchParams);

    long count();

    boolean existsByPurposeName(String purposeName);

    boolean existsByPurposeNameExcludingPurposeId(String purposeName, String excludePurposeId);
}
