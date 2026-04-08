package io.kaoto.forage.dr.engine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.support.RoutePolicySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.ExchangeSnapshotSerializer;
import io.kaoto.forage.dr.core.RecoveryStore;
import io.kaoto.forage.dr.core.SnapshotType;

/**
 * Route policy that tracks in-flight exchanges and drains them to a
 * {@link RecoveryStore} during graceful shutdown.
 *
 * <p>When Camel receives a SIGTERM, it calls {@link #onStop(Route)} on each route.
 * At that point, any exchanges still tracked as in-flight are serialized and
 * persisted so they can be replayed on the next startup.</p>
 */
public class ShutdownPersistenceRoutePolicy extends RoutePolicySupport {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownPersistenceRoutePolicy.class);

    private final RecoveryStore recoveryStore;
    private final ConcurrentMap<String, Exchange> inflightExchanges = new ConcurrentHashMap<>();

    public ShutdownPersistenceRoutePolicy(RecoveryStore recoveryStore) {
        this.recoveryStore = recoveryStore;
    }

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        inflightExchanges.put(exchange.getExchangeId(), exchange);
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {
        inflightExchanges.remove(exchange.getExchangeId());
    }

    @Override
    public void onStop(Route route) {
        if (inflightExchanges.isEmpty()) {
            LOG.debug("Route {} stopping with no in-flight exchanges", route.getRouteId());
            return;
        }

        LOG.info(
                "Route {} stopping — draining {} in-flight exchange(s) to recovery store",
                route.getRouteId(),
                inflightExchanges.size());

        int saved = 0;
        for (Exchange exchange : inflightExchanges.values()) {
            try {
                ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.SHUTDOWN);
                recoveryStore.saveShutdownExchange(snapshot);
                saved++;
            } catch (Exception e) {
                LOG.error(
                        "Failed to persist in-flight exchange {} during shutdown: {}",
                        exchange.getExchangeId(),
                        e.getMessage(),
                        e);
            }
        }

        LOG.info(
                "Route {} — saved {}/{} in-flight exchange(s) for recovery",
                route.getRouteId(),
                saved,
                inflightExchanges.size());
        inflightExchanges.clear();
    }
}
