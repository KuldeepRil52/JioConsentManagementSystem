package com.jio.vault.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jio.vault.constants.ErrorCode;
import com.jio.vault.dto.*;
import com.jio.vault.exception.CustomException;
import org.springframework.stereotype.Component;

@Component
public class ValidateBody {

    private final ObjectMapper objectMapper;

    public ValidateBody(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    public void validatePayload(SignPayloadRequest payloadString) {
        if (payloadString == null || payloadString.getPayloadAsString()==null || payloadString.getPayloadAsString().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Payload cannot be empty");
        }
        if ("{}".equals(payloadString.getPayloadAsString().trim())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Payload cannot be empty JSON");
        }
        if(payloadString.getPayloadAsString().length()<3){
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Payload cannot be an empty String");
        }
        try {
            objectMapper.readTree(payloadString.getPayloadAsString());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Invalid JSON payload");
        }
    }

    public void validateEncryptRequest(EncryptRequest encryptRequest) {
        if (encryptRequest == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Encrypt payload cannot be null");
        }
        if(encryptRequest.getBase64Text() == null || encryptRequest.getBase64Text().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Base64text cannot be empty");
        }
    }

    public void validateDecryptRequest(DecryptRequest decryptRequest) {
        if (decryptRequest == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Decrypt payload cannot be null");
        }
        if(decryptRequest.getCiphertext() == null || decryptRequest.getCiphertext().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Ciphertext cannot be empty");
        }
    }

    public void validateJwtRequest(VerifyJwtSignRequest jwtSignRequest) {
        if (jwtSignRequest == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "JWT payload cannot be null");
        }
        if(jwtSignRequest.getJwt() == null || jwtSignRequest.getJwt().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "JWT value cannot be empty");
        }
    }
}
