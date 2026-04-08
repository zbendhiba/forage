package io.kaoto.forage.dr.store.jdbc;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.h2.jdbcx.JdbcDataSource;
import io.kaoto.forage.core.util.config.ConfigStore;
import io.kaoto.forage.dr.core.RecoveryStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JdbcRecoveryStoreProvider Tests")
class JdbcRecoveryStoreProviderTest {

    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        ConfigStore.getInstance().reload();
        System.clearProperty("forage.dr.schema.auto.create");
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:provider_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;
    }

    @AfterEach
    void cleanUp() {
        System.clearProperty("forage.dr.schema.auto.create");
        ConfigStore.getInstance().reload();
    }

    @Nested
    @DisplayName("Create Method Tests")
    class CreateMethodTests {

        @Test
        @DisplayName("Should create RecoveryStore")
        void shouldCreateRecoveryStore() {
            JdbcRecoveryStoreProvider provider = new JdbcRecoveryStoreProvider();
            provider.setDataSource(dataSource);

            RecoveryStore store = provider.create(null);

            assertThat(store).isNotNull().isInstanceOf(JdbcRecoveryStore.class);
        }

        @Test
        @DisplayName("Should throw when DataSource not set")
        void shouldThrowWhenDataSourceNotSet() {
            JdbcRecoveryStoreProvider provider = new JdbcRecoveryStoreProvider();

            assertThatThrownBy(() -> provider.create(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DataSource not set");
        }

        @Test
        @DisplayName("Should initialize schema when auto-create is enabled")
        void shouldInitializeSchemaWhenAutoCreateEnabled() throws SQLException {
            JdbcRecoveryStoreProvider provider = new JdbcRecoveryStoreProvider();
            provider.setDataSource(dataSource);

            provider.create(null);

            assertThat(tableExists("FORAGE_DR_CHECKPOINT")).isTrue();
            assertThat(tableExists("FORAGE_DR_SHUTDOWN_EXCHANGE")).isTrue();
        }
    }

    @Nested
    @DisplayName("Schema Auto-Create Config Tests")
    class SchemaAutoCreateTests {

        @Test
        @DisplayName("Should skip schema initialization when disabled")
        void shouldSkipSchemaInitializationWhenDisabled() throws SQLException {
            System.setProperty("forage.dr.schema.auto.create", "false");
            try {
                JdbcRecoveryStoreProvider provider = new JdbcRecoveryStoreProvider();
                provider.setDataSource(dataSource);

                provider.create(null);

                assertThat(tableExists("FORAGE_DR_CHECKPOINT")).isFalse();
            } finally {
                System.clearProperty("forage.dr.schema.auto.create");
            }
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = dataSource.getConnection();
                ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
