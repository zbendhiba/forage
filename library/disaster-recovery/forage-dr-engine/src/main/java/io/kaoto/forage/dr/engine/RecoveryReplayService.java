package io.kaoto.forage.dr.engine;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.ExchangeSnapshotSerializer;
import io.kaoto.forage.dr.core.RecoveryStore;

/**
 * Replays persisted exchanges on startup. Called after the CamelContext
 * has fully started (via {@code CamelContextStartedEvent}).
 *
 * <p>Loads both checkpoints and shutdown-drained exchanges from the
 * {@link RecoveryStore} and re-injects them into their original routes
 * via {@link ProducerTemplate}.</p>
 */
public class RecoveryReplayService {

    private static final Logger LOG = LoggerFactory.getLogger(RecoveryReplayService.class);

    private final RecoveryStore recoveryStore;

    public RecoveryReplayService(RecoveryStore recoveryStore) {
        this.recoveryStore = recoveryStore;
    }

    /**
     * Replays all persisted exchanges into their original routes.
     *
     * @param camelContext the started CamelContext
     */
    public void replay(CamelContext camelContext) {
        Set<String> routeIds =
                camelContext.getRoutes().stream().map(Route::getRouteId).collect(Collectors.toSet());

        int totalReplayed = 0;

        // Replay shutdown-drained exchanges
        List<ExchangeSnapshot> shutdownExchanges = recoveryStore.loadShutdownExchanges();
        if (!shutdownExchanges.isEmpty()) {
            LOG.info("Found {} shutdown-drained exchange(s) to replay", shutdownExchanges.size());
            totalReplayed += replaySnapshots(camelContext, shutdownExchanges, routeIds);
        }

        // Replay checkpoints per route
        for (String routeId : routeIds) {
            List<ExchangeSnapshot> checkpoints = recoveryStore.loadCheckpoints(routeId);
            if (!checkpoints.isEmpty()) {
                LOG.info("Found {} checkpoint(s) to replay for route {}", checkpoints.size(), routeId);
                totalReplayed += replaySnapshots(camelContext, checkpoints, routeIds);
            }
        }

        if (totalReplayed > 0) {
            LOG.info("Recovery replay complete — {} exchange(s) replayed", totalReplayed);
        } else {
            LOG.debug("No exchanges to replay on startup");
        }
    }

    private int replaySnapshots(CamelContext camelContext, List<ExchangeSnapshot> snapshots, Set<String> routeIds) {
        int replayed = 0;

        try (ProducerTemplate producer = camelContext.createProducerTemplate()) {
            for (ExchangeSnapshot snapshot : snapshots) {
                String routeId = snapshot.getRouteId();

                if (!routeIds.contains(routeId)) {
                    LOG.warn("Skipping replay of exchange for route {} — route no longer exists", routeId);
                    continue;
                }

                try {
                    Exchange exchange = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);
                    Route route = camelContext.getRoute(routeId);
                    String endpointUri = route.getEndpoint().getEndpointUri();

                    producer.send(endpointUri, exchange);
                    replayed++;

                    LOG.debug(
                            "Replayed {} exchange into route {} (endpoint: {})",
                            snapshot.getSnapshotType(),
                            routeId,
                            endpointUri);
                } catch (Exception e) {
                    LOG.error("Failed to replay exchange for route {}: {}", routeId, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to create ProducerTemplate for replay: {}", e.getMessage(), e);
        }

        return replayed;
    }
}
