package com.jio.schedular.service;

import com.jio.schedular.constant.ErrorCodes;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.exception.SchedularException;
import com.jio.schedular.repository.SchedularStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedularRunTracker {

    private final SchedularStatsRepository schedularStatsRepository;

    /**
     * Fetch run info for all jobs within a tenant, optionally filtered by businessId and date range.
     */
    public List<SchedularStats> getAll(String tenantId,
                                       String businessId,
                                       Instant startDate,
                                       Instant endDate) throws SchedularException {

        log.info("[SchedularRunTracker] Fetching ALL jobs for tenantId={} filters -> businessId={}, start={}, end={}",
                tenantId, businessId, startDate, endDate);

        if (tenantId == null || tenantId.isBlank()) {
            throw new SchedularException(ErrorCodes.JCMP2001);
        }

        try {
            List<SchedularStats> results = schedularStatsRepository.findStatsByDateRangeAndBusinessId(
                    tenantId, startDate, endDate, businessId);

            if (results == null) {
                throw new SchedularException(ErrorCodes.JCMP3001);
            }

            filterResourcesByBusinessId(results, businessId);
            return results;

        } catch (SchedularException e) {
            throw e;
        } catch (Exception e) {
            log.error("DB fetch error for tenantId={}", tenantId, e);
            throw new SchedularException(ErrorCodes.JCMP3006);
        }
    }

    /**
     * Fetch run info for a specific job within a tenant, optionally filtered by businessId and date range.
     */
    public List<SchedularStats> getJob(String tenantId,
                                       String jobName,
                                       String businessId,
                                       Instant startDate,
                                       Instant endDate) throws SchedularException {

        log.info("[SchedularRunTracker] Fetching JOB={} for tenantId={} filters -> businessId={}, start={}, end={}",
                jobName, tenantId, businessId, startDate, endDate);

        if (tenantId == null || tenantId.isBlank()) {
            throw new SchedularException(ErrorCodes.JCMP2001);
        }

        try {
            List<SchedularStats> results = schedularStatsRepository.findStatsByJobName(
                    tenantId, jobName, startDate, endDate, businessId);

            if (results == null) {
                throw new SchedularException(ErrorCodes.JCMP3007);
            }

            filterResourcesByBusinessId(results, businessId);
            return results;

        } catch (SchedularException e) {
            throw e;
        } catch (Exception e) {
            log.error("DB fetch error while getting job {} for tenant {}", jobName, tenantId, e);
            throw new SchedularException(ErrorCodes.JCMP3006);
        }
    }

    /**
     * Filters resources list by businessId
     */
    private void filterResourcesByBusinessId(List<SchedularStats> results, String businessId) {
        if (businessId != null && !businessId.isEmpty()) {
            results.forEach(stat -> {
                if (stat.getResources() != null) {
                    stat.setResources(
                            stat.getResources().stream()
                                    .filter(r -> businessId.equals(r.getBusinessId()))
                                    .toList()
                    );
                }
            });
        }
    }
}