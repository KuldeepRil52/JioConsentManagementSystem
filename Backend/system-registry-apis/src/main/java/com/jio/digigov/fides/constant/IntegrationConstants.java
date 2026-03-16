package com.jio.digigov.fides.constant;

import java.util.List;

public final class IntegrationConstants {

    private IntegrationConstants() {}

    public static final String INTEGRATION_ID_PREFIX = "INT-DB-";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    public static final String DB_MONGODB = "MONGODB";
    public static final String DB_MYSQL = "MYSQL";

    public static final List<String> MANDATORY_DB_FIELDS = List.of(
            "host",
            "port",
            "username",
            "password",
            "database",
            "sshRequired"
    );
}