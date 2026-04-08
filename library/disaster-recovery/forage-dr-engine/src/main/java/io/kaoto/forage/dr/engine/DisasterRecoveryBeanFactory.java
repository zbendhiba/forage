package io.kaoto.forage.dr.engine;

import javax.sql.DataSource;

import java.util.List;
import java.util.ServiceLoader;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.core.common.BeanFactory;
import io.kaoto.forage.dr.core.RecoveryConfig;
import io.kaoto.forage.dr.core.RecoveryStore;
import io.kaoto.forage.dr.core.RecoveryStoreProvider;
import io.kaoto.forage.dr.store.jdbc.JdbcRecoveryStoreProvider;

/**
 * The orchestrator that wires all disaster recovery components together.
 * Discovered via ServiceLoader as a {@link BeanFactory}.
 *
 * <p>Defers initialization to {@code CamelContextStartedEvent} to ensure
 * all other BeanFactories (e.g., DataSourceBeanFactory) have already
 * registered their beans in the Camel registry.</p>
 */
public class DisasterRecoveryBeanFactory implements BeanFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DisasterRecoveryBeanFactory.class);

    private CamelContext camelContext;

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void configure() {
        RecoveryConfig config = new RecoveryConfig();

        if (!config.enabled()) {
            LOG.info("Disaster recovery is disabled (forage.dr.enabled=false)");
            return;
        }

        LOG.info("Disaster recovery enabled — deferring initialization to context start");

        camelContext.getManagementStrategy().addEventNotifier(new EventNotifierSupport() {
            @Override
            public void notify(CamelEvent event) throws Exception {
                if (event instanceof CamelEvent.CamelContextStartedEvent) {
                    initializeOnContextStarted(config);
                }
            }
        });
    }

    private void initializeOnContextStarted(RecoveryConfig config) {
        LOG.info("CamelContext started — initializing disaster recovery");

        // 1. Discover and create the recovery store
        RecoveryStore recoveryStore = createRecoveryStore(config);
        if (recoveryStore == null) {
            LOG.error("No RecoveryStoreProvider found on classpath. "
                    + "Disaster recovery requires a storage backend (e.g., forage-dr-store-jdbc). "
                    + "Disabling DR.");
            return;
        }

        // 2. Register shutdown persistence (graceful shutdown drain)
        if (config.shutdownPersistence()) {
            camelContext.addRoutePolicyFactory(new ShutdownPersistenceRoutePolicyFactory(recoveryStore));
            LOG.info("Shutdown persistence enabled — in-flight exchanges will be drained on shutdown");
        }

        // 3. Register checkpoint intercept strategy
        if (config.checkpointEnabled()) {
            camelContext
                    .getCamelContextExtension()
                    .addInterceptStrategy(new CheckpointInterceptStrategy(recoveryStore));
            LOG.info("Checkpoint/resume enabled — exchange state will be checkpointed at each processor");
        }

        // 4. Replay persisted exchanges
        if (config.replayOnStartup()) {
            RecoveryReplayService replayService = new RecoveryReplayService(recoveryStore);
            replayService.replay(camelContext);
        }

        // 5. Warn about in-memory AI providers
        if (config.aiStateCheckEnabled()) {
            AiStateRecoveryAdvisor.advise(camelContext);
        }

        // 6. Register event notifier for recovery observability
        camelContext.getManagementStrategy().addEventNotifier(new RecoveryEventNotifier());

        LOG.info(
                "Disaster recovery fully initialized (backend: {}, TTL: {}s)",
                config.storageBackend(),
                config.snapshotTtlSeconds());
    }

    private RecoveryStore createRecoveryStore(RecoveryConfig config) {
        List<ServiceLoader.Provider<RecoveryStoreProvider>> providers = findProviders(RecoveryStoreProvider.class);

        if (providers.isEmpty()) {
            return null;
        }

        for (ServiceLoader.Provider<RecoveryStoreProvider> provider : providers) {
            RecoveryStoreProvider storeProvider = provider.get();

            // Wire DataSource for JDBC providers
            if (storeProvider instanceof JdbcRecoveryStoreProvider) {
                DataSource dataSource =
                        camelContext.getRegistry().lookupByNameAndType(config.jdbcDatasourceName(), DataSource.class);

                if (dataSource == null) {
                    LOG.error(
                            "DataSource '{}' not found in Camel registry. "
                                    + "Ensure a JDBC DataSource provider (e.g., forage-jdbc-postgresql) "
                                    + "is on the classpath and configured.",
                            config.jdbcDatasourceName());
                    return null;
                }

                ((JdbcRecoveryStoreProvider) storeProvider).setDataSource(dataSource);
            }

            return storeProvider.create(null);
        }

        return null;
    }
}
