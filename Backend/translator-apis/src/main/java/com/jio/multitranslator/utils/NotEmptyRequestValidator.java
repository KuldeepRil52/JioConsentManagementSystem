package com.jio.multitranslator.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

public class NotEmptyRequestValidator implements ConstraintValidator<NotEmptyRequest, Object> {

    private static final Logger log = LoggerFactory.getLogger(NotEmptyRequestValidator.class);

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return false;

        try {
            // Use Bean introspection to safely get readable properties
            for (PropertyDescriptor pd : Introspector.getBeanInfo(value.getClass(), Object.class).getPropertyDescriptors()) {
                Object propertyValue = getPropertyValue(pd, value);
                if (propertyValue == null) {
                    continue; // skip null properties or properties we can't read
                }
                
                if (propertyValue instanceof String s) {
                    if (!s.isBlank()) {
                        return true;
                    }
                } else {
                    return true; // any non-String, non-null value counts
                }
            }
        } catch (IntrospectionException ex) {
            log.warn("Property access failed during validation: {}", ex.getMessage());
            return false;
        }

        return false; // all properties null or blank
    }

    /**
     * Gets the property value, returning null if the property cannot be read.
     */
    private Object getPropertyValue(PropertyDescriptor pd, Object value) {
        if (pd.getReadMethod() == null) {
            return null; // skip write-only properties
        }
        try {
            return pd.getReadMethod().invoke(value);
        } catch (ReflectiveOperationException e) {
            return null; // return null if we can't read the property
        }
    }
}
