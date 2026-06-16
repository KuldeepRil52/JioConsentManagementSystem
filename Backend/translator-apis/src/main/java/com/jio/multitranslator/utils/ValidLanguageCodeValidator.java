package com.jio.multitranslator.utils;

import com.jio.multitranslator.constant.Constants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidLanguageCodeValidator implements ConstraintValidator<ValidLanguageCode, String> {

    @Override
    public boolean isValid(String languageCode, ConstraintValidatorContext context) {
        if (languageCode == null || languageCode.isBlank()) {
            return false;
        }
        return Constants.SUPPORTED_LANGUAGES.contains(languageCode.toLowerCase());
    }
}
