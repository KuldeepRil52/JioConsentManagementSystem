package com.jio.auth.validation;

import java.util.Map;

import com.jio.auth.constants.ErrorCode;

import com.jio.auth.constants.HeaderFields;
import com.jio.auth.exception.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ValidateHeader {
    public void validateHeaders(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get(HeaderFields.SESSION_TOKEN))) {
            throw new CustomException(ErrorCode.NO_TOKEN_HEADER);
        }
    }

    public void validateSessionHeader(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get(HeaderFields.ACCESS_TOKEN))) {
            throw new CustomException(ErrorCode.MISSING_ACCESS_TOKEN);
        }
        if (!StringUtils.hasText(headers.get(HeaderFields.TENANT_ID))) {
            throw new CustomException(ErrorCode.MISSING_TENANT_ID);
        }
        if (!StringUtils.hasText(headers.get(HeaderFields.BUSINESS_ID))) {
            throw new CustomException(ErrorCode.MISSING_BUSINESS_ID);
        }
        if (!StringUtils.hasText(headers.get(HeaderFields.IDENTITY))) {
            throw new CustomException(ErrorCode.MISSING_IDENTITY);
        }
    }
    public void validateTokenHeader(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get(HeaderFields.TENANT_ID))) {
            throw new CustomException(ErrorCode.MISSING_TENANT_ID);
        }
        if (!StringUtils.hasText(headers.get(HeaderFields.BUSINESS_ID))) {
            throw new CustomException(ErrorCode.MISSING_BUSINESS_ID);
        }
    }

    public void validateBusinessId(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get(HeaderFields.BUSINESS_ID))) {
            throw new CustomException(ErrorCode.MISSING_BUSINESS_ID);
        }
    }

    public void validateTenantToken(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get(HeaderFields.SECRET_CODE))) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "missing the header "+ HeaderFields.SECRET_CODE);
        }
        if (!StringUtils.hasText(headers.get(HeaderFields.IDENTITY_VALUE))) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "missing the header "+ HeaderFields.IDENTITY_VALUE);
        }
    }


}

