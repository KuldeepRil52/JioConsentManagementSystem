package com.jio.schedular.exception;

public class VaultJwtSigningException extends RuntimeException {

    public VaultJwtSigningException(String message) {
        super(message);
    }

    public VaultJwtSigningException(String message, Throwable cause) {
        super(message, cause);
    }
}