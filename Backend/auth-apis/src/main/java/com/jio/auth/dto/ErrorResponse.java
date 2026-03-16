package com.jio.auth.dto;

public class ErrorResponse {
    private String errorCode;
    private String errorMessage;


    public ErrorResponse(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;

    }

    public String getErrorCode() {
        return errorCode;
    }
    public void setErrorCode(String code) {this.errorCode = code;}
    public String getErrorMessage() {return errorMessage;}
    public void setErrorMessage(String message) {this.errorMessage = message;}

}
