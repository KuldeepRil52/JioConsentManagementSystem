package com.jio.digigov.fides.model;

public class SystemExecutionResult {

    private final String systemId;
    private final boolean success;
    private final String failureReason;

    private SystemExecutionResult(String systemId, boolean success, String failureReason) {
        this.systemId = systemId;
        this.success = success;
        this.failureReason = failureReason;
    }

    public static SystemExecutionResult success(String systemId) {
        return new SystemExecutionResult(systemId, true, null);
    }

    public static SystemExecutionResult failure(String systemId, String reason) {
        return new SystemExecutionResult(systemId, false, reason);
    }

    public String getSystemId() {
        return systemId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }
}


