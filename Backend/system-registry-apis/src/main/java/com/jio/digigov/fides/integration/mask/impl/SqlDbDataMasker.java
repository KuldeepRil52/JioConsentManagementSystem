package com.jio.digigov.fides.integration.mask.impl;

import com.jio.digigov.fides.dto.CustomerIdentifiers;
import com.jio.digigov.fides.integration.mask.DbDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SqlDbDataMasker implements DbDataMasker {

    @Override
    @SuppressWarnings("unchecked")
    public void maskData(
            Map<String, Object> details,
            String datasetYaml,
            Set<String> dataItems,
            CustomerIdentifiers customerIdentifiers) {

        log.info("Starting SQL data masking");
        log.debug("Withdrawn dataItems: {}", dataItems);

        Map<String, Object> yaml = new Yaml().load(datasetYaml);

        // dataset is a MAP (same as Mongo)
        Map<String, Object> dataset =
                (Map<String, Object>) yaml.get("dataset");

        if (dataset == null) {
            log.warn("No dataset defined, skipping SQL masking");
            return;
        }

        List<Map<String, Object>> tables =
                (List<Map<String, Object>>) dataset.get("collections");

        if (tables == null || tables.isEmpty()) {
            log.warn("No collections defined, skipping SQL masking");
            return;
        }

        Set<String> normalizedWithdrawn =
                dataItems.stream()
                        .map(this::normalize)
                        .collect(Collectors.toSet());

        String jdbcUrl = buildJdbcUrl(details);
        String username = (String) details.get("username");
        String password = (String) details.get("password");
        String identityCategory = identityCategory(customerIdentifiers);
        log.info("jdbcUrl: {}", jdbcUrl);

        try (Connection connection =
                     DriverManager.getConnection(jdbcUrl, username, password)) {

            Set<String> userIds = new HashSet<>();
            String primaryTable = null;
            String identityColumn = null;

            /* -------------------------------------------------
             * PASS 1: Identify & mask PRIMARY table
             * ------------------------------------------------- */
            for (Map<String, Object> tableDef : tables) {

                List<Map<String, Object>> fields =
                        (List<Map<String, Object>>) tableDef.get("fields");

                if (fields == null) continue;

                Optional<Map<String, Object>> identityFieldOpt =
                        fields.stream()
                                .filter(f -> {
                                    List<String> cats =
                                            (List<String>) f.get("data_categories");
                                    return cats != null && cats.contains(identityCategory);
                                })
                                .findFirst();

                Optional<Map<String, Object>> userIdFieldOpt =
                        fields.stream()
                                .filter(f -> {
                                    List<String> cats =
                                            (List<String>) f.get("data_categories");
                                    return cats != null && cats.contains("user.unique_id");
                                })
                                .findFirst();

                if (identityFieldOpt.isPresent() && userIdFieldOpt.isPresent()) {

                    primaryTable = tableDef.get("name").toString();
                    identityColumn = identityFieldOpt.get().get("name").toString();
                    String userIdColumn = userIdFieldOpt.get().get("name").toString();

                    String selectSql =
                            "SELECT " + userIdColumn +
                                    " FROM " + primaryTable +
                                    " WHERE " + identityColumn + " = ?";

                    try (PreparedStatement ps =
                                 connection.prepareStatement(selectSql)) {

                        ps.setString(1, customerIdentifiers.getValue());
                        ResultSet rs = ps.executeQuery();

                        while (rs.next()) {
                            userIds.add(rs.getString(1));
                        }
                    }

                    maskTable(
                            connection,
                            primaryTable,
                            fields,
                            normalizedWithdrawn,
                            identityColumn + " = ?",
                            List.of(customerIdentifiers.getValue())
                    );

                    break; // only one primary table
                }
            }

            if (primaryTable == null || userIds.isEmpty()) {
                log.info(
                        "No primary table or users found for {}",
                        customerIdentifiers.getValue()
                );
                return;
            }

            /* -------------------------------------------------
             * PASS 2: Mask RELATED tables
             * ------------------------------------------------- */

            String inClause =
                    userIds.stream().map(x -> "?").collect(Collectors.joining(","));

            for (Map<String, Object> tableDef : tables) {

                String tableName = tableDef.get("name").toString();
                if (tableName.equals(primaryTable)) continue;

                List<Map<String, Object>> fields =
                        (List<Map<String, Object>>) tableDef.get("fields");

                if (fields == null) continue;

                Optional<Map<String, Object>> userIdFieldOpt =
                        fields.stream()
                                .filter(f -> {
                                    List<String> cats =
                                            (List<String>) f.get("data_categories");
                                    return cats != null &&
                                            cats.contains("user.unique_id");
                                })
                                .findFirst();

                if (userIdFieldOpt.isEmpty()) continue;

                String userIdColumn =
                        userIdFieldOpt.get().get("name").toString();

                maskTable(
                        connection,
                        tableName,
                        fields,
                        normalizedWithdrawn,
                        userIdColumn + " IN (" + inClause + ")",
                        new ArrayList<>(userIds)
                );
            }

        } catch (Exception e) {
            log.error("SQL data masking failed", e);
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------
     * Mask helper (Mongo-equivalent)
     * ------------------------------------------------- */
    private void maskTable(
            Connection connection,
            String tableName,
            List<Map<String, Object>> fields,
            Set<String> withdrawnItems,
            String whereClause,
            List<String> params) throws SQLException {

        Map<String, Integer> columnTypes = new HashMap<>();

        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs =
                     meta.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columnTypes.put(
                        rs.getString("COLUMN_NAME"),
                        rs.getInt("DATA_TYPE")
                );
            }
        }

        List<String> setClauses = new ArrayList<>();

        for (Map<String, Object> field : fields) {

            String columnName = field.get("name").toString();
            List<String> dataCategories =
                    (List<String>) field.get("data_categories");

            String joinedCategory =
                    dataCategories == null
                            ? null
                            : String.join("", dataCategories);

            String normalizedCategory = normalize(joinedCategory);

            boolean shouldMask =
                    dataCategories != null &&
                            withdrawnItems.contains(normalizedCategory);

            if (!shouldMask) continue;

            Integer sqlType = columnTypes.get(columnName);
            if (sqlType == null) continue;

            setClauses.add(columnName + " = " + sqlMaskValue(sqlType));
        }

        if (setClauses.isEmpty()) {
            log.info("No fields to mask for table {}", tableName);
            return;
        }

        String sql =
                "UPDATE " + tableName +
                        " SET " + String.join(", ", setClauses) +
                        " WHERE " + whereClause;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            int updated = ps.executeUpdate();
            log.info("MASK | Table={} | Rows={}", tableName, updated);
        }
    }

    /* -------------------------------------------------
     * Mask values (type-safe like Mongo $switch)
     * ------------------------------------------------- */
    private String sqlMaskValue(int sqlType) {
        return switch (sqlType) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR ->
                    "'REDACTED'";

            case Types.INTEGER, Types.BIGINT, Types.DECIMAL,
                 Types.NUMERIC, Types.FLOAT, Types.DOUBLE ->
                    "0";

            case Types.BOOLEAN, Types.BIT ->
                    "FALSE";

            case Types.DATE, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE ->
                    "NULL";

            default ->
                    "NULL";
        };
    }

    private String buildJdbcUrl(Map<String, Object> details) {

        String dbType = details.get("dbType").toString().toUpperCase();
        String host = details.get("host").toString();
        int port = Integer.parseInt(details.get("port").toString());
        String database = details.get("database").toString();

        return switch (dbType) {
            case "POSTGRES", "POSTGRESQL" ->
                    "jdbc:postgresql://" + host + ":" + port + "/" + database;
            case "MYSQL" ->
                    "jdbc:mysql://" + host + ":" + port + "/" + database;
            case "ORACLE" ->
                    "jdbc:oracle:thin:@" + host + ":" + port + ":" + database;
            case "MSSQL" ->
                    "jdbc:sqlserver://" + host + ":" + port +
                            ";databaseName=" + database;
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported SQL DB type: " + dbType);
        };
    }

    private static String identityCategory(CustomerIdentifiers identifier) {
        return switch (identifier.getType()) {
            case EMAIL -> "Email Address";
            case MOBILE -> "Phone Number (Mobile / Landline)";
        };
    }

    private String normalize(String s) {
        return s == null
                ? null
                : s.toLowerCase()
                .replaceAll("[\\s()\\[\\],/]", "");
    }
}
