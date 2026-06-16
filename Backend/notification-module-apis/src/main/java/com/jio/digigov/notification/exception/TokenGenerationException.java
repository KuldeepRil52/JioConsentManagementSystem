package com.jio.digigov.notification.exception;

public class TokenGenerationException extends RuntimeException {
    
    public TokenGenerationException(String message) {
        super(message);
    }
    
    public TokenGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static TokenGenerationException forNetworkType(String networkType, String reason) {
        return new TokenGenerationException(
            String.format("Failed to generate token for network type %s: %s", networkType, reason));
    }
    
    public static TokenGenerationException forError(String error, String errorDescription) {
        return new TokenGenerationException(
            String.format("Token generation failed: %s - %s", error, errorDescription));
    }
}