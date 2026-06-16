package com.jio.partnerportal.exception;

import java.io.Serial;

public class PartnerPortalException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public PartnerPortalException(String errorCode) {
        super("Exception Occurred");
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

}
