package com.jio.digigov.fides.integration.test;

import java.util.Map;

public interface DbConnectionTester {

    Map<String, Object> test(Map<String, Object> connectionDetails);
}