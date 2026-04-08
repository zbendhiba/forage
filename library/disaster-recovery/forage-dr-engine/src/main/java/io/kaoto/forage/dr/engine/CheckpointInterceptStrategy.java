package io.kaoto.forage.dr.engine;

import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.spi.InterceptStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.dr.core.RecoveryStore;

/**
 * Intercept strategy that wraps every processor with a {@link CheckpointProcessor}.
 * This snapshots exchange state before each processing step, providing checkpoint/resume
 * capability for disaster recovery.
 *
 * <p>Registered on the CamelContext by {@code DisasterRecoveryBeanFactory}.</p>
 */
public class CheckpointInterceptStrategy implements InterceptStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(CheckpointInterceptStrategy.class);

    private final RecoveryStore recoveryStore;

    public CheckpointInterceptStrategy(RecoveryStore recoveryStore) {
        this.recoveryStore = recoveryStore;
    }

    @Override
    public Processor wrapProcessorInInterceptors(
            CamelContext context, NamedNode definition, Processor target, Processor nextTarget) {

        LOG.trace("Wrapping processor {} with checkpoint", definition.getId());
        return new CheckpointProcessor(target, recoveryStore);
    }
}
