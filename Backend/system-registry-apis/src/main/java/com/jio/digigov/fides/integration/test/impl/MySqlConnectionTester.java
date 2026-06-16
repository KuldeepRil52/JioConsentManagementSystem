package com.jio.digigov.fides.integration.test.impl;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jio.digigov.fides.integration.test.DbConnectionTester;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class MySqlConnectionTester implements DbConnectionTester {

    @Override
    public Map<String, Object> test(Map<String, Object> details) {

        String host = get(details, "host");
        int port = Integer.parseInt(get(details, "port"));
        String database = get(details, "database");

        String username = details.get("username") != null
                ? details.get("username").toString()
                : "";

        String password = details.get("password") != null
                ? details.get("password").toString()
                : "";

        boolean sshRequired = Boolean.parseBoolean(
                String.valueOf(details.getOrDefault("sshRequired", false))
        );

        Session sshSession = null;

        try {

            // ===============================
            // SSH TUNNEL (IF REQUIRED)
            // ===============================
            if (sshRequired) {

                String sshHost = get(details, "sshHost");
                int sshPort = Integer.parseInt(
                        String.valueOf(details.getOrDefault("sshPort", 22))
                );
                String sshUser = get(details, "sshUser");
                String sshPassword = get(details, "sshPassword");
                int localForwardPort = Integer.parseInt(
                        get(details, "localForwardPort")
                );

                sshSession = openSshTunnel(
                        sshHost,
                        sshPort,
                        sshUser,
                        sshPassword,
                        host,
                        port,
                        localForwardPort
                );

                host = "localhost";
                port = localForwardPort;
            }

            // ===============================
            // MYSQL CONNECTION
            // ===============================
            String url = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&connectTimeout=5000",
                    host, port, database
            );

            log.info("Testing MySQL connection to {}:{}/{}", host, port, database);

            Class.forName("com.mysql.cj.jdbc.Driver");

            Properties props = new Properties();
            if (!username.isBlank()) {
                props.put("user", username);
            }
            if (!password.isBlank()) {
                props.put("password", password);
            }

            try (Connection connection = DriverManager.getConnection(url, props)) {
                // connection test only
            }

            return Map.of(
                    "success", true,
                    "message", "MySQL connection successful",
                    "database", database
            );

        } catch (Exception e) {

            log.error("MySQL connection test failed", e);

            return Map.of(
                    "success", false,
                    "message", "MySQL connection failed",
                    "error", e.getMessage()
            );

        } finally {
            if (sshSession != null && sshSession.isConnected()) {
                sshSession.disconnect();
                log.info("SSH tunnel closed");
            }
        }
    }

    // ===============================
    // SSH TUNNEL
    // ===============================
    private Session openSshTunnel(
            String sshHost,
            int sshPort,
            String sshUser,
            String sshPassword,
            String remoteHost,
            int remotePort,
            int localPort
    ) throws Exception {

        JSch jsch = new JSch();
        Session session = jsch.getSession(sshUser, sshHost, sshPort);
        session.setPassword(sshPassword);

        Properties cfg = new Properties();
        cfg.put("StrictHostKeyChecking", "no");
        session.setConfig(cfg);

        session.connect(15000);

        session.setPortForwardingL(
                localPort,
                remoteHost,
                remotePort
        );

        log.info("SSH tunnel established: localhost:{} → {}:{}",
                localPort, remoteHost, remotePort);

        return session;
    }

    // ===============================
    // REQUIRED FIELD HELPER
    // ===============================
    private String get(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return val.toString();
    }
}
