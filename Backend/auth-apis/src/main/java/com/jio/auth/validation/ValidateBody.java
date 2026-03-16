package com.jio.auth.validation;

import com.jio.auth.constants.BodyFields;
import com.jio.auth.constants.ErrorCode;
import com.jio.auth.exception.CustomException;
import org.springframework.stereotype.Component;
import java.util.Map;



@Component
public class ValidateBody {
    public void ValidateClaimsBody(Map<String, String> payload) {
        if(!payload.containsKey(BodyFields.SUBJECT) || payload.get(BodyFields.SUBJECT) == null ||payload.get(BodyFields.SUBJECT).isEmpty() ){
            throw new CustomException(ErrorCode.MISSING_SUB);
        }
        if(!payload.containsKey(BodyFields.ISSUER)  || payload.get(BodyFields.ISSUER) == null ||payload.get(BodyFields.ISSUER).isEmpty() ){
            throw new CustomException(ErrorCode.MISSING_ISSUER);
        }
        if(!payload.containsKey(BodyFields.TENANT_ID)  || payload.get(BodyFields.TENANT_ID) == null ||payload.get(BodyFields.TENANT_ID).isEmpty() ){
            throw new CustomException(ErrorCode.MISSING_TENANT_ID);
        }
        if(!payload.containsKey(BodyFields.BUSINESS_ID)  || payload.get(BodyFields.BUSINESS_ID) == null || payload.get(BodyFields.BUSINESS_ID).isEmpty() ){
            throw new CustomException(ErrorCode.MISSING_BUSINESS_ID);
        }
    }

    public void validateSessionBody(Map<String, String> payload) {

        if (!payload.containsKey(BodyFields.IDENTITY) || isEmpty(payload.get(BodyFields.IDENTITY))) {
            throw new CustomException(ErrorCode.MISSING_IDENTITY);
        }

        if (!payload.containsKey(BodyFields.IDENTITY_TYPE) || isEmpty(payload.get(BodyFields.IDENTITY_TYPE))) {
            throw new CustomException(ErrorCode.MISSING_IDENTITY_TYPE);
        }
    }
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
