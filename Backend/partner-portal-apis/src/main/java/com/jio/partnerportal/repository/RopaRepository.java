package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.RopaRecord;

import java.util.List;
import java.util.Map;

public interface RopaRepository {

    RopaRecord save(RopaRecord ropaRecord);

    RopaRecord findByRopaId(String ropaId);

    List<RopaRecord> findByBusinessId(String businessId);

    List<RopaRecord> findRopaByParams(Map<String, String> searchParams);

    long count();

    boolean existsByRopaId(String ropaId);

    void deleteByRopaId(String ropaId);

    List<RopaRecord> findAllRopa();
}
