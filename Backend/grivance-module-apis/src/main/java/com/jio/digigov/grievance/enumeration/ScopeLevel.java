package com.jio.digigov.grievance.enumeration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.jio.digigov.grievance.util.SafeEnumDeserializer;

@JsonDeserialize(using = ScopeLevel.Deserializer.class)
public enum ScopeLevel {
    TENANT,
    BUSINESS;

    public static class Deserializer extends SafeEnumDeserializer<ScopeLevel> {
        public Deserializer() {
            super(ScopeLevel.class);
        }
    }
}