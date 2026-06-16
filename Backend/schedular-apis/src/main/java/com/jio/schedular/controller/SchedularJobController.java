package com.jio.schedular.controller;

import com.jio.schedular.constant.ErrorCodes;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.entity.SchedularStats;
import com.jio.schedular.exception.SchedularException;
import com.jio.schedular.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class SchedularJobController {

    private final SchedularRunTracker schedularRunTracker;

    /**
     * Get run info for all registered scheduler jobs (with optional filters).
     */
    @GetMapping("/stats")
    public ResponseEntity<List<SchedularStats>> getAllJobs(
            @RequestHeader("tenantId") String tenantId,
            @RequestHeader(value = "businessId", required = false) String businessId,
            @RequestHeader(value = "startDate", required = false) String startDateStr,
            @RequestHeader(value = "endDate", required = false) String endDateStr) throws SchedularException {
        
        log.info("Received request for all jobs: tenantId={}, businessId={}, start={}, end={}", tenantId, businessId, startDateStr, endDateStr);

        if(tenantId == null || tenantId.isEmpty()){
            log.error("tenantId is missing in the headers");
            throw new SchedularException(ErrorCodes.JCMP2001);
        }

        Instant startDate = parseInstant(startDateStr, false);
        Instant endDate = parseInstant(endDateStr, true);

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new SchedularException(ErrorCodes.JCMP3008);
        }

        List<SchedularStats> result;
        try {
            result = schedularRunTracker.getAll(tenantId, businessId, startDate, endDate);
        } catch (Exception e) {
            log.error("Error fetching scheduler stats for tenantId={} businessId={}", tenantId, businessId);
            throw new SchedularException(ErrorCodes.JCMP3006);
        }

        if (result == null || result.isEmpty()) {
            throw new SchedularException(ErrorCodes.JCMP3001);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get run info for a specific job (with optional filters).
     */
    @GetMapping("/stats/{jobName}")
    public ResponseEntity<List<SchedularStats>> getJobDetails(
            @PathVariable String jobName,
            @RequestHeader("tenantId") String tenantId,
            @RequestHeader(value = "businessId", required = false) String businessId,
            @RequestHeader(value = "startDate", required = false) String startDateStr,
            @RequestHeader(value = "endDate", required = false) String endDateStr) throws SchedularException {
        
        log.info("Received request for job '{}': tenantId={}, businessId={}, start={}, end={}", jobName, tenantId, businessId, startDateStr, endDateStr);

        if(tenantId == null || tenantId.isEmpty()){
            log.error("tenantId is missing in the headers");
            throw new SchedularException(ErrorCodes.JCMP2001);
        }
        
        Instant startDate = parseInstant(startDateStr, false);
        Instant endDate = parseInstant(endDateStr, true);

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new SchedularException(ErrorCodes.JCMP3008);
        }

        List<SchedularStats> jobList;
        try {
            jobList = schedularRunTracker.getJob(tenantId, jobName, businessId, startDate, endDate);
        } catch (Exception e) {
            log.error("Error fetching job details for job={}, tenantId={} businessId={}", jobName, tenantId, businessId);
            throw new SchedularException(ErrorCodes.JCMP3006);
        }

        if (jobList == null) {
            throw new SchedularException(ErrorCodes.JCMP3007);
        }
        return ResponseEntity.ok(jobList);
    }

    /**
     * Parse ISO date strings safely.
     */
    private Instant parseInstant(String dateStr, boolean isEndDate) throws SchedularException {
        if (dateStr == null) return null;
        try {
            // Try full ISO-8601 timestamp first
            return Instant.parse(dateStr);
        } catch (DateTimeParseException e) {
            // Try yyyy-MM-dd format
            try {
                LocalDate localDate = LocalDate.parse(dateStr);
                return isEndDate ?
                    localDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC)
                    : localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ex) {
                log.warn("Invalid date header '{}', ignoring filter", dateStr);
                throw new SchedularException(ErrorCodes.JCMP4004);
            }
        }
    }
}