package com.jio.multitranslator.utils;

import com.jio.multitranslator.constant.Constants;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isEmptyObject(Object obj) {
        return isEmptyObject(obj, new IdentityHashMap<>());
    }

    private static boolean isEmptyObject(Object obj, Map<Object, Boolean> visited) {
        if (obj == null) return true;

        if (visited.containsKey(obj)) return true;
        visited.put(obj, true);

        if (isEmptyCollectionOrMap(obj)) {
            return true;
        }

        if (isEmptyString(obj)) {
            return true;
        }

        if (obj.getClass().isPrimitive()) {
            return false;
        }

        return checkFieldsEmpty(obj, visited);
    }

    /**
     * Checks if the object is an empty collection or map.
     */
    private static boolean isEmptyCollectionOrMap(Object obj) {
        if (obj instanceof Collection<?> coll) {
            return coll.isEmpty();
        }
        if (obj instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }

    /**
     * Checks if the object is an empty string.
     */
    private static boolean isEmptyString(Object obj) {
        if (obj instanceof String str) {
            return str.isBlank();
        }
        return false;
    }

    /**
     * Checks if all fields of the object are empty.
     */
    private static boolean checkFieldsEmpty(Object obj, Map<Object, Boolean> visited) {
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }
                
                if (!isFieldEmpty(field, obj, visited)) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            log.warn("Unable to access field securely: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Security exception accessing field: {}", e.getMessage());
        } catch (Exception ex) {
            log.error("Reflection error: {}", ex.getMessage());
        }
        return true;
    }

    /**
     * Determines if a field should be skipped during validation.
     */
    private static boolean shouldSkipField(Field field) {
        String packageName = field.getDeclaringClass().getPackageName();
        boolean isSynthetic = field.isSynthetic();
        boolean isJavaPackage = packageName.startsWith(Constants.JAVA_PACKAGE_PREFIX);
        boolean isApplicationPackage = packageName.startsWith(Constants.APPLICATION_PACKAGE_PREFIX);
        boolean isAccessible = field.trySetAccessible();
        
        if (isSynthetic || isJavaPackage || !isApplicationPackage || !isAccessible) {
            if (!isSynthetic && isAccessible) {
                log.debug("Cannot access field: {}", field.getName());
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if a field's value is empty.
     */
    private static boolean isFieldEmpty(Field field, Object obj, Map<Object, Boolean> visited) 
            throws IllegalAccessException {
        Object value = field.get(obj);
        return value == null || isEmptyObject(value, visited);
    }
}
