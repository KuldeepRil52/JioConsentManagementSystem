package com.jio.digigov.grievance.enumeration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.jio.digigov.grievance.util.SafeEnumDeserializer;

@JsonDeserialize(using = GrievanceUpdateStatus.Deserializer.class)
public enum GrievanceUpdateStatus {
    INPROCESS,
    RESOLVED;

    public static class Deserializer extends SafeEnumDeserializer<GrievanceUpdateStatus> {
        public Deserializer() {
            super(GrievanceUpdateStatus.class);
        }
    }
}