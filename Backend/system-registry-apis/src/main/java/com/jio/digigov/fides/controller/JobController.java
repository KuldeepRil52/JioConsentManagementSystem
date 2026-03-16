package com.jio.digigov.fides.controller;

import com.jio.digigov.fides.entity.ConsentWithdrawalJob;
import com.jio.digigov.fides.service.impl.JobServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobServiceImpl jobService;

    @GetMapping("/{jobId}")
    public ResponseEntity<ConsentWithdrawalJob> getJobStatus(
            @PathVariable String jobId,
            @RequestHeader("X-TENANT-ID") String tenantId,
            @RequestHeader("X-BUSINESS-ID") String businessId) {

        return new ResponseEntity<ConsentWithdrawalJob>(jobService.getJob(jobId, tenantId, businessId), HttpStatus.OK);
    }
}