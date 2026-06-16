package com.jio.digigov.fides.integration.test.impl;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jio.digigov.fides.integration.test.DbConnectionTester;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.Map;
import java.util.Properties;

@Slf4j
public class MongoDbConnectionTester implements DbConnectionTester {

    @Override
    public Map<String, Object> test(Map<String, Object> details) {

        String host = get(details, "host");
        int port = Integer.parseInt(get(details, "port"));
        String database = get(details, "database");

        String username = details.get("username") != null
                ? details.get("username").toString()
                : null;

        String password = details.get("password") != null
                ? details.get("password").toString()
                : null;

        boolean sshRequired = Boolean.parseBoolean(
                String.valueOf(details.getOrDefault("sshRequired", false))
        );

        Session sshSession = null;

        try {

            // ===============================
            // SSH TUNNEL (NO HARDCODING)
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

                // connect MongoDB via tunnel
                host = "localhost";
                port = localForwardPort;
            }

            // ===============================
            // MONGODB CONNECTION
            // ===============================
            try (MongoClient client =
                         createClient(host, port, database, username, password)) {

                client.getDatabase(database)
                        .runCommand(new Document("ping", 1));

                return Map.of(
                        "success", true,
                        "message", "MongoDB connection successful",
                        "database", database
                );
            }

        } catch (Exception e) {

            log.error("MongoDB connection test failed", e);

            return Map.of(
                    "success", false,
                    "message", "MongoDB connection failed",
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
    // SSH TUNNEL (GENERIC)
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
    // MONGODB CLIENT
    // ===============================
    private MongoClient createClient(
            String host,
            int port,
            String database,
            String username,
            String password
    ) {

        String uri;

        if (username != null && !username.isBlank()
                && password != null && !password.isBlank()) {

            uri = String.format(
                    "mongodb://%s:%s@%s:%d/?authSource=admin&authMechanism=SCRAM-SHA-256",
                    username,
                    password,
                    host,
                    port
            );

        } else {

            uri = String.format(
                    "mongodb://%s:%d/?directConnection=true",
                    host,
                    port
            );
        }

        log.debug("MongoDB URI used: {}", uri.replaceAll(":.*@", ":***@"));

        return MongoClients.create(uri);
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
