package com.travelbird.support;

import java.util.Map;

final class LocalMySqlSettings {
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    private LocalMySqlSettings(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    static LocalMySqlSettings from(Map<String, String> environment) {
        String username = required(environment, "TEST_DB_USERNAME");
        String password = required(environment, "TEST_DB_PASSWORD");
        String host = environment.getOrDefault("TEST_DB_HOST", "127.0.0.1");
        if (!host.matches("[a-zA-Z0-9.-]+")) {
            throw new IllegalArgumentException("Invalid TEST_DB_HOST; use a host name, not a JDBC URL");
        }
        int port;
        try {
            port = Integer.parseInt(environment.getOrDefault("TEST_DB_PORT", "3306"));
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException("Invalid TEST_DB_PORT");
        }
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid TEST_DB_PORT");
        return new LocalMySqlSettings(host, port, username, password);
    }

    private static String required(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + key + " for local MySQL integration tests");
        return value;
    }

    String jdbcUrl(String schema) {
        if (!schema.isEmpty()) LocalMySqlDatabase.requireGeneratedName(schema);
        return "jdbc:mysql://" + host + ":" + port + "/" + schema
                + "?serverTimezone=UTC&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=30000";
    }

    String username() { return username; }
    String password() { return password; }
    @Override public String toString() { return "LocalMySqlSettings[credentials redacted]"; }
}
