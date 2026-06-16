package com.jio.digigov.notification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Circuit breaker implementation for resilient external service calls.
 *
 * This implementation provides circuit breaker pattern functionality to prevent
 * cascading failures when external services are unavailable or responding slowly.
 * It automatically transitions between CLOSED, OPEN, and HALF_OPEN states based
 * on failure rates and response times.
 *
 * Circuit Breaker States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Service is considered down, requests fail fast
 * - HALF_OPEN: Testing if service has recovered
 *
 * Configuration:
 * - Failure threshold: 60% failure rate triggers circuit opening
 * - Minimum request threshold: 10 requests before evaluation
 * - Recovery timeout: 30 seconds before attempting recovery
 * - Slow call threshold: 5 seconds response time threshold
 *
 * @author Notification Service Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class CircuitBreakerUtil {

    // Circuit breaker configuration
    private static final int FAILURE_THRESHOLD_PERCENTAGE = 60;
    private static final int MINIMUM_REQUESTS_THRESHOLD = 10;
    private static final long RECOVERY_TIMEOUT_SECONDS = 30;
    private static final long SLOW_CALL_THRESHOLD_MS = 5000;
    private static final int HALF_OPEN_MAX_CALLS = 5;

    // Circuit breaker states per service
    private final ConcurrentHashMap<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Circuit breaker state enumeration
     */
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Failing fast
        HALF_OPEN  // Testing recovery
    }

    /**
     * Circuit breaker state holder
     */
    private static class CircuitBreakerState {
        private volatile State state = State.CLOSED;
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger slowCallCount = new AtomicInteger(0);
        private final AtomicInteger halfOpenCallCount = new AtomicInteger(0);
        private volatile LocalDateTime lastFailureTime;
        private volatile LocalDateTime stateChangeTime = LocalDateTime.now();
        private final AtomicLong totalResponseTime = new AtomicLong(0);

        public void recordSuccess(long responseTime) {
            requestCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);

            if (responseTime > SLOW_CALL_THRESHOLD_MS) {
                slowCallCount.incrementAndGet();
            }

            if (state == State.HALF_OPEN) {
                halfOpenCallCount.incrementAndGet();
            }
        }

        public void recordFailure() {
            requestCount.incrementAndGet();
            failureCount.incrementAndGet();
            lastFailureTime = LocalDateTime.now();

            if (state == State.HALF_OPEN) {
                halfOpenCallCount.incrementAndGet();
            }
        }

        public double getFailureRate() {
            int total = requestCount.get();
            if (total < MINIMUM_REQUESTS_THRESHOLD) {
                return 0.0;
            }
            return (double) failureCount.get() / total * 100;
        }

        public double getSlowCallRate() {
            int total = requestCount.get();
            if (total == 0) {
                return 0.0;
            }
            return (double) slowCallCount.get() / total * 100;
        }

        public double getAverageResponseTime() {
            int total = requestCount.get();
            if (total == 0) {
                return 0.0;
            }
            return (double) totalResponseTime.get() / total;
        }

        public void reset() {
            requestCount.set(0);
            failureCount.set(0);
            slowCallCount.set(0);
            halfOpenCallCount.set(0);
            totalResponseTime.set(0);
            stateChangeTime = LocalDateTime.now();
        }

        public void transitionTo(State newState) {
            State oldState = this.state;
            this.state = newState;
            this.stateChangeTime = LocalDateTime.now();

            if (newState == State.CLOSED || newState == State.HALF_OPEN) {
                reset();
            }
        }
    }

    /**
     * Execute a call with circuit breaker protection
     */
    public <T> T executeWithCircuitBreaker(String serviceName, Supplier<T> call, Supplier<T> fallback) {
        CircuitBreakerState breaker = circuitBreakers.computeIfAbsent(serviceName, k -> new CircuitBreakerState());

        // Check if circuit is open
        if (breaker.state == State.OPEN) {
            if (shouldAttemptRecovery(breaker)) {
                breaker.transitionTo(State.HALF_OPEN);
                log.info("Circuit breaker for {} transitioned to HALF_OPEN", serviceName);
            } else {
                log.warn("Circuit breaker for {} is OPEN, executing fallback", serviceName);
                return fallback.get();
            }
        }

        // Check if too many calls in half-open state
        if (breaker.state == State.HALF_OPEN && breaker.halfOpenCallCount.get() >= HALF_OPEN_MAX_CALLS) {
            log.warn("Circuit breaker for {} is HALF_OPEN with max calls reached, executing fallback", serviceName);
            return fallback.get();
        }

        // Execute the call
        long startTime = System.currentTimeMillis();
        try {
            T result = call.get();
            long responseTime = System.currentTimeMillis() - startTime;

            recordSuccess(serviceName, breaker, responseTime);
            return result;

        } catch (Exception e) {
            recordFailure(serviceName, breaker, e);
            return fallback.get();
        }
    }

    /**
     * Record successful call
     */
    private void recordSuccess(String serviceName, CircuitBreakerState breaker, long responseTime) {
        breaker.recordSuccess(responseTime);

        // Transition from HALF_OPEN to CLOSED if recovery is successful
        if (breaker.state == State.HALF_OPEN && breaker.halfOpenCallCount.get() >= HALF_OPEN_MAX_CALLS) {
            if (breaker.getFailureRate() < FAILURE_THRESHOLD_PERCENTAGE) {
                breaker.transitionTo(State.CLOSED);
                log.info("Circuit breaker for {} transitioned to CLOSED (recovered)", serviceName);
            } else {
                breaker.transitionTo(State.OPEN);
                log.warn("Circuit breaker for {} transitioned back to OPEN (recovery failed)", serviceName);
            }
        }

        // Check if circuit should open due to failures
        evaluateCircuitState(serviceName, breaker);

        log.debug("Circuit breaker success for {}: responseTime={}ms, failureRate={}%",
                serviceName, responseTime, breaker.getFailureRate());
    }

    /**
     * Record failed call
     */
    private void recordFailure(String serviceName, CircuitBreakerState breaker, Exception e) {
        breaker.recordFailure();

        // Transition from HALF_OPEN to OPEN on failure
        if (breaker.state == State.HALF_OPEN) {
            breaker.transitionTo(State.OPEN);
            log.warn("Circuit breaker for {} transitioned to OPEN (half-open failure)", serviceName);
        }

        // Check if circuit should open due to failures
        evaluateCircuitState(serviceName, breaker);

        log.warn("Circuit breaker failure for {}: error={}, failureRate={}%",
                serviceName, e.getMessage(), breaker.getFailureRate());
    }

    /**
     * Evaluate if circuit state should change
     */
    private void evaluateCircuitState(String serviceName, CircuitBreakerState breaker) {
        if (breaker.state == State.CLOSED) {
            if (breaker.requestCount.get() >= MINIMUM_REQUESTS_THRESHOLD) {
                double failureRate = breaker.getFailureRate();
                double slowCallRate = breaker.getSlowCallRate();

                if (failureRate >= FAILURE_THRESHOLD_PERCENTAGE || slowCallRate >= FAILURE_THRESHOLD_PERCENTAGE) {
                    breaker.transitionTo(State.OPEN);
                    log.warn("Circuit breaker for {} transitioned to OPEN: failureRate={}%, slowCallRate={}%",
                            serviceName, failureRate, slowCallRate);
                }
            }
        }
    }

    /**
     * Check if recovery should be attempted
     */
    private boolean shouldAttemptRecovery(CircuitBreakerState breaker) {
        if (breaker.lastFailureTime == null) {
            return true;
        }

        long secondsSinceLastFailure = ChronoUnit.SECONDS.between(breaker.lastFailureTime, LocalDateTime.now());
        return secondsSinceLastFailure >= RECOVERY_TIMEOUT_SECONDS;
    }

    /**
     * Get circuit breaker state for monitoring
     */
    public CircuitBreakerMetrics getMetrics(String serviceName) {
        CircuitBreakerState breaker = circuitBreakers.get(serviceName);
        if (breaker == null) {
            return new CircuitBreakerMetrics(serviceName, State.CLOSED, 0, 0.0, 0.0, 0.0);
        }

        return new CircuitBreakerMetrics(
                serviceName,
                breaker.state,
                breaker.requestCount.get(),
                breaker.getFailureRate(),
                breaker.getSlowCallRate(),
                breaker.getAverageResponseTime()
        );
    }

    /**
     * Get all circuit breaker metrics
     */
    public java.util.Map<String, CircuitBreakerMetrics> getAllMetrics() {
        java.util.Map<String, CircuitBreakerMetrics> metrics = new ConcurrentHashMap<>();
        circuitBreakers.forEach((serviceName, breaker) -> {
            metrics.put(serviceName, getMetrics(serviceName));
        });
        return metrics;
    }

    /**
     * Reset circuit breaker for a service
     */
    public void resetCircuitBreaker(String serviceName) {
        CircuitBreakerState breaker = circuitBreakers.get(serviceName);
        if (breaker != null) {
            breaker.transitionTo(State.CLOSED);
            log.info("Circuit breaker for {} manually reset to CLOSED", serviceName);
        }
    }

    /**
     * Circuit breaker metrics data class
     */
    public static class CircuitBreakerMetrics {
        public final String serviceName;
        public final State state;
        public final int requestCount;
        public final double failureRate;
        public final double slowCallRate;
        public final double averageResponseTime;

        public CircuitBreakerMetrics(String serviceName, State state, int requestCount,
                                   double failureRate, double slowCallRate, double averageResponseTime) {
            this.serviceName = serviceName;
            this.state = state;
            this.requestCount = requestCount;
            this.failureRate = failureRate;
            this.slowCallRate = slowCallRate;
            this.averageResponseTime = averageResponseTime;
        }

        @Override
        public String toString() {
            return String.format("CircuitBreaker[%s]: state=%s, requests=%d, failureRate=%.1f%%, " +
                    "slowCallRate=%.1f%%, avgResponseTime=%.1fms",
                    serviceName, state, requestCount, failureRate, slowCallRate, averageResponseTime);
        }
    }
}