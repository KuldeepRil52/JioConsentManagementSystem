package com.jio.digigov.grievance.enumeration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.jio.digigov.grievance.util.SafeEnumDeserializer;

@JsonDeserialize(using = GrievanceStatus.Deserializer.class)
public enum GrievanceStatus {
    INPROCESS,
    L1_ESCALATED,
    L2_ESCALATED,
    RESOLVED,
    NEW;

    public static class Deserializer extends SafeEnumDeserializer<GrievanceStatus> {
        public Deserializer() {
            super(GrievanceStatus.class);
        }
    }
}