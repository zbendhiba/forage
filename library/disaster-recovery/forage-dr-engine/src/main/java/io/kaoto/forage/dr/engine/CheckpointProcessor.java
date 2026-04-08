package io.kaoto.forage.dr.engine;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.processor.DelegateAsyncProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.dr.core.ExchangeSnapshotSerializer;
import io.kaoto.forage.dr.core.RecoveryStore;
import io.kaoto.forage.dr.core.SnapshotType;

/**
 * Wraps a processor to checkpoint exchange state before delegation.
 * If the pod dies mid-processing, the last checkpoint shows how far
 * the exchange progressed through the route.
 *
 * <p>Extends {@link DelegateAsyncProcessor} to preserve Camel's async engine.</p>
 */
public class CheckpointProcessor extends DelegateAsyncProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CheckpointProcessor.class);

    private final RecoveryStore recoveryStore;

    public CheckpointProcessor(Processor processor, RecoveryStore recoveryStore) {
        super(processor);
        this.recoveryStore = recoveryStore;
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        try {
            recoveryStore.saveCheckpoint(ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT));
        } catch (Exception e) {
            LOG.warn(
                    "Failed to checkpoint exchange {}, continuing processing: {}",
                    exchange.getExchangeId(),
                    e.getMessage());
        }

        return super.process(exchange, callback);
    }
}
