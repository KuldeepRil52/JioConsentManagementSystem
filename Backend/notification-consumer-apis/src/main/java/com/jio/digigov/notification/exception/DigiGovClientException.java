package com.jio.digigov.notification.exception;

public class DigiGovClientException extends RuntimeException {
    
    private final String operation;
    private final int statusCode;
    
    public DigiGovClientException(String operation, int statusCode, String message) {
        super(message);
        this.operation = operation;
        this.statusCode = statusCode;
    }
    
    public DigiGovClientException(String operation, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.statusCode = statusCode;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public static DigiGovClientException forOnboard(int statusCode, String message) {
        return new DigiGovClientException("onboard", statusCode, message);
    }
    
    public static DigiGovClientException forApprove(int statusCode, String message) {
        return new DigiGovClientException("approve", statusCode, message);
    }
    
    public static DigiGovClientException forOtpInit(int statusCode, String message) {
        return new DigiGovClientException("otp_init", statusCode, message);
    }
    
    public static DigiGovClientException forOtpVerify(int statusCode, String message) {
        return new DigiGovClientException("otp_verify", statusCode, message);
    }
    
    public static DigiGovClientException forSendNotification(int statusCode, String message) {
        return new DigiGovClientException("send_notification", statusCode, message);
    }
}