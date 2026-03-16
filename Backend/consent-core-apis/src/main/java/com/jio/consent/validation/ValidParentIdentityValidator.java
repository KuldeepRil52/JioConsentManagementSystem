package com.jio.consent.validation;

import com.jio.consent.constant.ErrorCodes;
import com.jio.consent.dto.Request.CreateParentalConsentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidParentIdentityValidator implements ConstraintValidator<ValidParentIdentity, CreateParentalConsentRequest> {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    @Override
    public boolean isValid(CreateParentalConsentRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        String parentIdentityType = request.getParentIdentityType();
        String parentIdentity = request.getParentIdentity();

        if (parentIdentityType == null || parentIdentity == null || parentIdentity.isBlank()) {
            return true; // Let @NotBlank handle these cases
        }

        if ("MOBILE".equalsIgnoreCase(parentIdentityType)) {
            if (!MOBILE_PATTERN.matcher(parentIdentity.trim()).matches()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(ErrorCodes.JCMP1076)
                        .addPropertyNode("parentIdentity")
                        .addConstraintViolation();
                return false;
            }
        } else if ("EMAIL".equalsIgnoreCase(parentIdentityType)) {
            if (!EMAIL_PATTERN.matcher(parentIdentity.trim()).matches()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(ErrorCodes.JCMP1077)
                        .addPropertyNode("parentIdentity")
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
