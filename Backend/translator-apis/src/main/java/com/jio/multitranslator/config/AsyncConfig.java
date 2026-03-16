package com.jio.multitranslator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for non-blocking operations.
 * Used for transaction logging and other fire-and-forget operations.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 5;
    private static final int QUEUE_CAPACITY = 100;
    private static final String THREAD_NAME_PREFIX = "translator-async-";

    @Bean(name = "transactionExecutor")
    public Executor transactionExecutor() {
        log.info("Configuring async executor - CorePoolSize: {}, MaxPoolSize: {}, QueueCapacity: {}", 
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        
        return executor;
    }
}
