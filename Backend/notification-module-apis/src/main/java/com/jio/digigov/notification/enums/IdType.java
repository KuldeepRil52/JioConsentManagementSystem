package com.jio.digigov.notification.enums;

/**
 * Enumeration for ID types used in notifications
 */
public enum IdType {
    PHONE(3),
    EMAIL(4),
    AADHAAR(1),
    PAN(2);
    
    private final int code;
    
    IdType(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    /**
     * Get IdType from numeric code
     */
    public static IdType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IdType idType : values()) {
            if (idType.code == code) {
                return idType;
            }
        }
        return null;
    }
}