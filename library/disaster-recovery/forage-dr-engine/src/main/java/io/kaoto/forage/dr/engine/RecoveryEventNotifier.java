package io.kaoto.forage.dr.engine;

import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event notifier that logs disaster recovery events.
 *
 * <p>Currently a logging-only stub. Future versions may emit metrics/traces
 * via Micrometer or OpenTelemetry when an observability bridge is added.</p>
 */
public class RecoveryEventNotifier extends EventNotifierSupport {

    private static final Logger LOG = LoggerFactory.getLogger(RecoveryEventNotifier.class);

    @Override
    public void notify(CamelEvent event) throws Exception {
        if (event instanceof CamelEvent.ExchangeCompletedEvent) {
            LOG.trace(
                    "Exchange completed: {}",
                    ((CamelEvent.ExchangeCompletedEvent) event).getExchange().getExchangeId());
        } else if (event instanceof CamelEvent.ExchangeFailedEvent) {
            LOG.debug(
                    "Exchange failed: {}",
                    ((CamelEvent.ExchangeFailedEvent) event).getExchange().getExchangeId());
        }
    }
}
