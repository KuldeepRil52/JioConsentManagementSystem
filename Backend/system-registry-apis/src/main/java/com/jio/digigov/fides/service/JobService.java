package com.jio.digigov.fides.service;

import com.jio.digigov.fides.entity.ConsentWithdrawalJob;

public interface JobService {

    /**
     * Retrieves the status of a job by its identifier.
     *
     * @param jobId      the identifier of the job
     * @param tenantId   tenant identifier (header)
     * @param businessId business identifier (header)
     * @return the ConsentWithdrawalJob representing the job status
     */
    ConsentWithdrawalJob getJob(String jobId, String tenantId, String businessId);
}