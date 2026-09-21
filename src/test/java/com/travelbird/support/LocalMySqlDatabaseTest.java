package com.travelbird.support;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalMySqlDatabaseTest {
    @Test void settingsRejectMissingCredentialsWithoutExposingValues() {
        assertThatThrownBy(() -> LocalMySqlSettings.from(Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("TEST_DB_USERNAME");
        assertThatThrownBy(() -> LocalMySqlSettings.from(Map.of("TEST_DB_USERNAME", "test-user")))
                .hasMessageContaining("TEST_DB_PASSWORD").hasMessageNotContaining("test-user");
    }
    @Test void hostAndPortCannotInjectDatabaseOrJdbcOptions() {
        for (String host : new String[]{"localhost/travelbird", "localhost?user=x", "localhost;DROP", "localhost:3306"}) {
            assertThatThrownBy(() -> LocalMySqlSettings.from(Map.of("TEST_DB_HOST", host,
                    "TEST_DB_USERNAME", "test-user", "TEST_DB_PASSWORD", "test-password")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining(host);
        }
        assertThatThrownBy(() -> LocalMySqlSettings.from(Map.of("TEST_DB_PORT", "0",
                "TEST_DB_USERNAME", "test-user", "TEST_DB_PASSWORD", "test-password")))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void refusesDevelopmentNamesAndPrefixOnlyNames() {
        for (String name : new String[]{"travelbird", "mysql", "travelbird_test", "travelbird_test_user", "travelbird_test_" + "a".repeat(32) + "`;DROP"}) {
            assertThatThrownBy(() -> LocalMySqlDatabase.requireGeneratedName(name)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void freshDatabaseIsOwnedAndDroppedOnlyOnce() throws Exception {
        var c=mock(Connection.class); var statement=mock(Statement.class); var metadata=mock(DatabaseMetaData.class);
        when(c.createStatement()).thenReturn(statement);when(c.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");when(metadata.getDatabaseMajorVersion()).thenReturn(8);
        var settings=LocalMySqlSettings.from(Map.of("TEST_DB_USERNAME","test-user","TEST_DB_PASSWORD","test-password"));
        var database=new LocalMySqlDatabase(settings,(url,user,password)->c);
        assertThat(database.schema()).matches("travelbird_test_[a-f0-9]{32}");
        assertThat(database.jdbcUrl()).contains("/"+database.schema()+"?").doesNotContain("/travelbird?");
        database.destroy(); database.destroy();
        verify(statement).executeUpdate("CREATE DATABASE `"+database.schema()+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        verify(statement,times(1)).executeUpdate("DROP DATABASE `"+database.schema()+"`");
        assertThat(settings.toString()).doesNotContain("test-password", "test-user");
    }
    @Test void createFailureNeverDropsAnUnownedSchemaOrLeaksCredentials() throws Exception {
        var c=mock(Connection.class);var statement=mock(Statement.class);var metadata=mock(DatabaseMetaData.class);
        when(c.createStatement()).thenReturn(statement);when(c.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");when(metadata.getDatabaseMajorVersion()).thenReturn(8);
        when(statement.executeUpdate(startsWith("CREATE DATABASE"))).thenThrow(new java.sql.SQLException("secret-password", "42000", 1044));
        var settings=LocalMySqlSettings.from(Map.of("TEST_DB_USERNAME","test-user","TEST_DB_PASSWORD","secret-password"));
        assertThatThrownBy(()->new LocalMySqlDatabase(settings,(url,user,password)->c))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("1044").hasMessageNotContaining("secret-password").hasNoCause();
        verify(statement,never()).executeUpdate(startsWith("DROP"));
    }
    @Test void createSuccessFollowedByResourceCloseFailureStillCleansOwnedSchema() throws Exception {
        var c=mock(Connection.class);var statement=mock(Statement.class);var metadata=mock(DatabaseMetaData.class);
        when(c.createStatement()).thenReturn(statement);when(c.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("MySQL");when(metadata.getDatabaseMajorVersion()).thenReturn(8);
        doThrow(new java.sql.SQLException("private detail","08000",1)).doNothing().when(statement).close();
        var settings=LocalMySqlSettings.from(Map.of("TEST_DB_USERNAME","test-user","TEST_DB_PASSWORD","test-password"));
        assertThatThrownBy(()->new LocalMySqlDatabase(settings,(url,user,password)->c)).hasMessageNotContaining("private detail");
        verify(statement).executeUpdate(startsWith("DROP DATABASE `travelbird_test_"));
    }
    @Test void contextCacheKeySeparatesTestClassesAndIgnoresUnitTests() {
        var factory=new LocalMySqlContextCustomizerFactory();
        assertThat(factory.createContextCustomizer(FirstSuite.class,java.util.List.of()))
                .isEqualTo(factory.createContextCustomizer(FirstSuite.class,java.util.List.of()))
                .isNotEqualTo(factory.createContextCustomizer(SecondSuite.class,java.util.List.of()));
        assertThat(factory.createContextCustomizer(LocalMySqlDatabaseTest.class,java.util.List.of())).isNull();
    }
    @org.springframework.boot.test.context.SpringBootTest static class FirstSuite {}
    @org.springframework.boot.test.context.SpringBootTest static class SecondSuite {}
}
