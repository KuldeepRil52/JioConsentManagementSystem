package com.jio.schedular.exception;

public class VaultEncryptionException extends RuntimeException {

    public VaultEncryptionException(String message) {
        super(message);
    }

    public VaultEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}