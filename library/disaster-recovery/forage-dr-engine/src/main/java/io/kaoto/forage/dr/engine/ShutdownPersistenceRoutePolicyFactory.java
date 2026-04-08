package io.kaoto.forage.dr.engine;

import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.dr.core.RecoveryStore;

/**
 * Factory that creates a {@link ShutdownPersistenceRoutePolicy} for every route.
 * Registered on the CamelContext by {@code DisasterRecoveryBeanFactory}.
 */
public class ShutdownPersistenceRoutePolicyFactory implements RoutePolicyFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownPersistenceRoutePolicyFactory.class);

    private final RecoveryStore recoveryStore;

    public ShutdownPersistenceRoutePolicyFactory(RecoveryStore recoveryStore) {
        this.recoveryStore = recoveryStore;
    }

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode route) {
        LOG.debug("Creating shutdown persistence policy for route {}", routeId);
        return new ShutdownPersistenceRoutePolicy(recoveryStore);
    }
}
