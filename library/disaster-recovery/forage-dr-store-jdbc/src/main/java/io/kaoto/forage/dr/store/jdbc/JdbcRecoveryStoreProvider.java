package io.kaoto.forage.dr.store.jdbc;

import javax.sql.DataSource;

import io.kaoto.forage.core.annotations.ForageBean;
import io.kaoto.forage.dr.core.RecoveryConfig;
import io.kaoto.forage.dr.core.RecoveryStore;
import io.kaoto.forage.dr.core.RecoveryStoreProvider;

/**
 * Creates {@link JdbcRecoveryStore} instances backed by a JDBC DataSource.
 *
 * <p>The DataSource must be set via {@link #setDataSource(DataSource)} before
 * calling {@link #create(String)}. The engine is responsible for looking up
 * the DataSource from the Camel registry and providing it here.</p>
 */
@ForageBean(
        value = "jdbc-recovery-store",
        components = {},
        description = "JDBC-backed disaster recovery store",
        feature = "Disaster Recovery",
        configClass = RecoveryConfig.class)
public class JdbcRecoveryStoreProvider implements RecoveryStoreProvider {

    private DataSource dataSource;

    /**
     * Sets the DataSource to use for creating recovery stores.
     * Must be called before {@link #create(String)}.
     *
     * @param dataSource the JDBC DataSource
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public RecoveryStore create(String id) {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not set on JdbcRecoveryStoreProvider. "
                    + "The engine must call setDataSource() before create().");
        }

        RecoveryConfig config = new RecoveryConfig(id);
        if (config.schemaAutoCreate()) {
            SchemaInitializer.initialize(dataSource);
        }

        return new JdbcRecoveryStore(dataSource, config.snapshotTtlSeconds());
    }
}
