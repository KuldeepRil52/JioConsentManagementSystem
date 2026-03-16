package com.jio.schedular.repository;

import com.jio.schedular.entity.SchedularStats;

import java.time.Instant;
import java.util.List;

public interface SchedularStatsRepository {

    void saveTenantSummary(String tenantId, SchedularStats stats);

    List<SchedularStats> findStatsByDateRangeAndBusinessId(String tenantId, Instant startDate, Instant endDate, String businessId);

    List<SchedularStats> findStatsByJobName(String tenantId, String jobName, Instant startDate, Instant endDate, String businessId);
}