package com.jio.digigov.fides.integration.factory;

import com.jio.digigov.fides.integration.mask.impl.MongoDbDataMasker;
import com.jio.digigov.fides.integration.mask.impl.SqlDbDataMasker;
import com.jio.digigov.fides.integration.mask.DbDataMasker;

/**
 * Factory to resolve DB-specific data masking implementation
 */
public final class DbDataMaskerFactory {

    private DbDataMaskerFactory() {
        // prevent instantiation
    }

    public static DbDataMasker getMasker(String dbType) {

        if (dbType == null) {
            throw new IllegalArgumentException("dbType cannot be null");
        }

        return switch (dbType.toUpperCase()) {
            case "MONGODB" -> new MongoDbDataMasker();
            case "POSTGRES", "POSTGRESQL", "MYSQL", "ORACLE", "MSSQL" ->
                    new SqlDbDataMasker();
            default -> throw new IllegalArgumentException(
                    "Unsupported DB type for masking: " + dbType
            );
        };
    }
}
