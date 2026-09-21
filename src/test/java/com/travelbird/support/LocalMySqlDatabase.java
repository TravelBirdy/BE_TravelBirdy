package com.travelbird.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.beans.factory.DisposableBean;

final class LocalMySqlDatabase implements DisposableBean {
    @FunctionalInterface interface Connector {
        Connection open(String url, String username, String password) throws SQLException;
    }

    private final LocalMySqlSettings settings;
    private final Connector connector;
    private final String schema = "travelbird_test_" + UUID.randomUUID().toString().replace("-", "");
    private boolean owned;

    LocalMySqlDatabase(LocalMySqlSettings settings) {
        this(settings, DriverManager::getConnection);
    }

    LocalMySqlDatabase(LocalMySqlSettings settings, Connector connector) {
        this.settings = settings;
        this.connector = connector;
        requireGeneratedName(schema);
        try (Connection connection = openServer()) {
            var metadata = connection.getMetaData();
            if (!"MySQL".equals(metadata.getDatabaseProductName()) || metadata.getDatabaseMajorVersion() != 8) {
                throw new IllegalStateException("Local integration tests require MySQL 8");
            }
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
                owned = true;
            }
        } catch (SQLException exception) {
            if (owned) {
                try { destroy(); }
                catch (IllegalStateException cleanupFailure) {
                    throw new IllegalStateException(failure("create", exception).getMessage()
                            + "; " + cleanupFailure.getMessage());
                }
            }
            throw failure("create", exception);
        }
    }

    static void requireGeneratedName(String candidate) {
        if (candidate == null || !candidate.matches("travelbird_test_[a-f0-9]{32}")) {
            throw new IllegalArgumentException("Refusing non-generated test database name");
        }
    }

    private Connection openServer() throws SQLException {
        return connector.open(settings.jdbcUrl(""), settings.username(), settings.password());
    }

    String schema() { return schema; }
    String jdbcUrl() { return settings.jdbcUrl(schema); }

    @Override public synchronized void destroy() {
        if (!owned) return;
        requireGeneratedName(schema);
        try (Connection connection = openServer(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE `" + schema + "`");
            owned = false;
            System.out.println("LOCAL_MYSQL_DROPPED " + schema);
        } catch (SQLException exception) {
            throw failure("drop", exception);
        }
    }

    private static IllegalStateException failure(String operation, SQLException exception) {
        // JDBC messages can include credentials or server connection details. Do not retain the cause.
        return new IllegalStateException("Local test database " + operation + " failed: SQLState="
                + exception.getSQLState() + ", code=" + exception.getErrorCode());
    }
}
