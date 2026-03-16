package com.jio.digigov.fides.integration.factory;

import com.jio.digigov.fides.constant.IntegrationConstants;
import com.jio.digigov.fides.integration.test.DbConnectionTester;
import com.jio.digigov.fides.integration.test.impl.MongoDbConnectionTester;
import com.jio.digigov.fides.integration.test.impl.MySqlConnectionTester;

public class DbConnectionTestFactory {

    public static DbConnectionTester getTester(String dbType) {

        return switch (dbType) {
            case IntegrationConstants.DB_MONGODB -> new MongoDbConnectionTester();
            case IntegrationConstants.DB_MYSQL -> new MySqlConnectionTester();
            default -> throw new IllegalArgumentException("Unsupported DB Type: " + dbType);
        };
    }
}