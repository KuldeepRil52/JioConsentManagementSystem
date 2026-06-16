package com.jio.digigov.fides.service;

import com.jio.digigov.fides.entity.ConsentWithdrawalJob;

/**
 * Async processor for consent withdrawal jobs
 */
public interface ConsentAsyncProcessor {

    /**
     * Processes a consent withdrawal job asynchronously.
     *
     * @param job consent withdrawal job
     */
    void process(ConsentWithdrawalJob job);
}