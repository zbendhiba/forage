package io.kaoto.forage.dr.store.jdbc;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto-creates the disaster recovery database tables on startup.
 * Controlled by the {@code forage.dr.schema.auto.create} config property.
 *
 * <p>For production environments with restricted DB permissions, disable auto-create
 * and apply the migration scripts in {@code db/migration/} manually.</p>
 */
public final class SchemaInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaInitializer.class);

    private static final String CREATE_CHECKPOINT_TABLE = "CREATE TABLE IF NOT EXISTS forage_dr_checkpoint ("
            + "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "exchange_id VARCHAR(255), "
            + "route_id VARCHAR(255) NOT NULL, "
            + "body BYTEA, "
            + "headers BYTEA, "
            + "properties BYTEA, "
            + "snapshot_type VARCHAR(20) NOT NULL, "
            + "created_at BIGINT NOT NULL, "
            + "partial BOOLEAN NOT NULL DEFAULT FALSE"
            + ")";

    private static final String CREATE_CHECKPOINT_ROUTE_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_dr_checkpoint_route ON forage_dr_checkpoint (route_id)";

    private static final String CREATE_CHECKPOINT_CREATED_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_dr_checkpoint_created ON forage_dr_checkpoint (created_at)";

    private static final String CREATE_SHUTDOWN_TABLE = "CREATE TABLE IF NOT EXISTS forage_dr_shutdown_exchange ("
            + "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "exchange_id VARCHAR(255), "
            + "route_id VARCHAR(255) NOT NULL, "
            + "body BYTEA, "
            + "headers BYTEA, "
            + "properties BYTEA, "
            + "snapshot_type VARCHAR(20) NOT NULL, "
            + "created_at BIGINT NOT NULL, "
            + "partial BOOLEAN NOT NULL DEFAULT FALSE"
            + ")";

    private static final String CREATE_SHUTDOWN_CREATED_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_dr_shutdown_created ON forage_dr_shutdown_exchange (created_at)";

    private SchemaInitializer() {}

    /**
     * Creates the DR tables if they do not exist.
     *
     * @param dataSource the DataSource to use
     */
    public static void initialize(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_CHECKPOINT_TABLE);
            stmt.execute(CREATE_CHECKPOINT_ROUTE_INDEX);
            stmt.execute(CREATE_CHECKPOINT_CREATED_INDEX);
            stmt.execute(CREATE_SHUTDOWN_TABLE);
            stmt.execute(CREATE_SHUTDOWN_CREATED_INDEX);
            LOG.info("Disaster recovery schema initialized");
        } catch (SQLException e) {
            LOG.error("Failed to initialize disaster recovery schema: {}", e.getMessage(), e);
        }
    }
}
