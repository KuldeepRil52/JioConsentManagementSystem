package com.jio.digigov.grievance.enumeration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.jio.digigov.grievance.util.SafeEnumDeserializer;

@JsonDeserialize(using = Status.Deserializer.class)
public enum Status {
    DRAFT,
    PUBLISHED;

    public static class Deserializer extends SafeEnumDeserializer<Status> {
        public Deserializer() {
            super(Status.class);
        }
    }
}