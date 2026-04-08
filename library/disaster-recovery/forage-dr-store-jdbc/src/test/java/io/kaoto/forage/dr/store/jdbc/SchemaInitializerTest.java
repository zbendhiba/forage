package io.kaoto.forage.dr.store.jdbc;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.h2.jdbcx.JdbcDataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("SchemaInitializer Tests")
class SchemaInitializerTest {

    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:schema_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;
    }

    @Nested
    @DisplayName("Table Creation Tests")
    class TableCreationTests {

        @Test
        @DisplayName("Should create checkpoint table")
        void shouldCreateCheckpointTable() throws SQLException {
            SchemaInitializer.initialize(dataSource);

            assertThat(tableExists("FORAGE_DR_CHECKPOINT")).isTrue();
        }

        @Test
        @DisplayName("Should create shutdown exchange table")
        void shouldCreateShutdownExchangeTable() throws SQLException {
            SchemaInitializer.initialize(dataSource);

            assertThat(tableExists("FORAGE_DR_SHUTDOWN_EXCHANGE")).isTrue();
        }

        @Test
        @DisplayName("Should create indexes")
        void shouldCreateIndexes() throws SQLException {
            SchemaInitializer.initialize(dataSource);

            assertThat(indexExists("IDX_DR_CHECKPOINT_ROUTE")).isTrue();
            assertThat(indexExists("IDX_DR_CHECKPOINT_CREATED")).isTrue();
            assertThat(indexExists("IDX_DR_SHUTDOWN_CREATED")).isTrue();
        }
    }

    @Nested
    @DisplayName("Idempotency Tests")
    class IdempotencyTests {

        @Test
        @DisplayName("Should be idempotent — running twice should not throw")
        void shouldBeIdempotent() throws SQLException {
            SchemaInitializer.initialize(dataSource);

            assertThatCode(() -> SchemaInitializer.initialize(dataSource)).doesNotThrowAnyException();

            assertThat(tableExists("FORAGE_DR_CHECKPOINT")).isTrue();
            assertThat(tableExists("FORAGE_DR_SHUTDOWN_EXCHANGE")).isTrue();
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = dataSource.getConnection();
                ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private boolean indexExists(String indexName) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            for (String table : new String[] {"FORAGE_DR_CHECKPOINT", "FORAGE_DR_SHUTDOWN_EXCHANGE"}) {
                try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, table, false, false)) {
                    while (rs.next()) {
                        if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
