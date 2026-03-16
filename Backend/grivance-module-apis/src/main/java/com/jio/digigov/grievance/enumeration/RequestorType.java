package com.jio.digigov.grievance.enumeration;

public enum RequestorType {
    DATAPROCESSOR,
    DATAFIDUCIARY,
    OTHER;

    public static RequestorType fromString(String value) {
        for (RequestorType type : RequestorType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return OTHER;
    }
}

