package com.jio.vault.validation;

import java.util.Map;

import com.jio.vault.constants.ErrorCode;
import com.jio.vault.constants.HeaderConstants;
import com.jio.vault.exception.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ValidateHeader {
    public void validateHeaders(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get("tenant-id"))) {
            throw new CustomException(ErrorCode.TENANT_ID_EMPTY);
        }
        if (!StringUtils.hasText(headers.get("business-id"))) {
            throw new CustomException(ErrorCode.BUSINESS_ID_EMPTY);
        }
        if (!StringUtils.hasText(headers.get("key-id"))) {
            throw new CustomException(ErrorCode.KEY_ID_EMPTY);
        }
    }

    public void validateHeadersExceptKeyId(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get("tenant-id"))) {
            throw new CustomException(ErrorCode.TENANT_ID_EMPTY);
        }
        if (!StringUtils.hasText(headers.get("business-id"))) {
            throw new CustomException(ErrorCode.BUSINESS_ID_EMPTY);
        }

    }

    public void validateEncryptHeaders(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get("tenant-id"))) {
            throw new CustomException(ErrorCode.TENANT_ID_EMPTY);
        }
        if (!StringUtils.hasText(headers.get("business-id"))) {
            throw new CustomException(ErrorCode.BUSINESS_ID_EMPTY);
        }
        if (!StringUtils.hasText(headers.get("data-category-type"))) {
            throw new CustomException(ErrorCode.DATA_CATEGORY_EMPTY);
        }
        if (!StringUtils.hasText(headers.get("data-category-value"))) {
            throw new CustomException(ErrorCode.DATA_CATEGORY_VAL_EMPTY);
        }

    }

    public void validateDecryptHeaders(Map<String, String> headers) {
        if (!StringUtils.hasText(headers.get("tenant-id"))) {
            throw new CustomException(ErrorCode.TENANT_ID_EMPTY);
        }
        if (!StringUtils.hasText(headers.get("business-id"))) {
            throw new CustomException(ErrorCode.BUSINESS_ID_EMPTY);
        }
        if(!StringUtils.hasText(headers.get(HeaderConstants.UUID))) {
            throw new CustomException(ErrorCode.UUID_EMPTY);
        }


    }


}
