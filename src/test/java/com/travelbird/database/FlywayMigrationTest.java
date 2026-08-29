package com.travelbird.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationCreatesApprovedColumnsAndConstraints() throws Exception {
        assertThat(columnExists("users", "status")).isTrue();
        assertThat(columnExists("events", "tour_api_content_id")).isTrue();
        assertThat(uniqueIndexExists("events", "uk_events_tour_api_content_id")).isTrue();
    }

    @Test
    void migrationCreatesExpectedDeleteRules() throws Exception {
        assertThat(deleteRule("user_social_accounts", "fk_user_social_accounts_user")).isEqualTo("CASCADE");
        assertThat(deleteRule("posts", "fk_posts_representative_file")).isEqualTo("SET NULL");
        assertThat(deleteRule("trips", "fk_trips_user")).isEqualTo("RESTRICT");
        assertThat(deleteRule("events", "fk_events_sigungu")).isEqualTo("RESTRICT");
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData().getColumns(
                 connection.getCatalog(),
                 null,
                 tableName.toUpperCase(Locale.ROOT),
                 columnName.toUpperCase(Locale.ROOT)
             )) {
            return resultSet.next();
        }
    }

    private boolean uniqueIndexExists(String tableName, String indexName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData().getIndexInfo(
                 connection.getCatalog(),
                 null,
                 tableName.toUpperCase(Locale.ROOT),
                 true,
                 false
             )) {
            while (resultSet.next()) {
                String currentIndexName = resultSet.getString("INDEX_NAME");
                boolean nonUnique = resultSet.getBoolean("NON_UNIQUE");
                if (indexName.equalsIgnoreCase(currentIndexName) && !nonUnique) {
                    return true;
                }
            }
            return false;
        }
    }

    private String deleteRule(String tableName, String fkName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData().getImportedKeys(
                 connection.getCatalog(),
                 null,
                 tableName.toUpperCase(Locale.ROOT)
             )) {
            while (resultSet.next()) {
                String currentFkName = resultSet.getString("FK_NAME");
                if (fkName.equalsIgnoreCase(currentFkName)) {
                    short deleteRule = resultSet.getShort("DELETE_RULE");
                    if (deleteRule == DatabaseMetaData.importedKeyCascade) {
                        return "CASCADE";
                    }
                    if (deleteRule == DatabaseMetaData.importedKeySetNull) {
                        return "SET NULL";
                    }
                    if (deleteRule == DatabaseMetaData.importedKeyNoAction || deleteRule == DatabaseMetaData.importedKeyRestrict) {
                        return "RESTRICT";
                    }
                    return "UNKNOWN";
                }
            }
        }
        fail("Missing foreign key: " + fkName);
        return "UNKNOWN";
    }
}
