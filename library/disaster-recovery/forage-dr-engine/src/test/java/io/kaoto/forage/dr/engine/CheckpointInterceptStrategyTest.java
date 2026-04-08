package io.kaoto.forage.dr.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.LogDefinition;
import org.apache.camel.support.DefaultExchange;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.RecoveryStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CheckpointInterceptStrategy Tests")
class CheckpointInterceptStrategyTest {

    private CamelContext camelContext;
    private LogDefinition stubDefinition;

    @BeforeEach
    void setUp() {
        camelContext = new DefaultCamelContext();
        stubDefinition = new LogDefinition("test-checkpoint");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (camelContext != null) {
            camelContext.close();
        }
    }

    @Nested
    @DisplayName("Processor Wrapping Tests")
    class ProcessorWrappingTests {

        @Test
        @DisplayName("Should wrap processor with CheckpointProcessor")
        void shouldWrapProcessorWithCheckpointProcessor() {
            CheckpointInterceptStrategy strategy = new CheckpointInterceptStrategy(new StubRecoveryStore());
            Processor target = exchange -> {};

            Processor wrapped = strategy.wrapProcessorInInterceptors(camelContext, stubDefinition, target, null);

            assertThat(wrapped).isInstanceOf(CheckpointProcessor.class);
        }

        @Test
        @DisplayName("Should preserve target processor — delegate is called")
        void shouldPreserveTargetProcessor() {
            StubRecoveryStore stubStore = new StubRecoveryStore();
            CheckpointInterceptStrategy strategy = new CheckpointInterceptStrategy(stubStore);
            RecordingProcessor target = new RecordingProcessor();

            Processor wrapped = strategy.wrapProcessorInInterceptors(camelContext, stubDefinition, target, null);
            Exchange exchange = new DefaultExchange(camelContext);
            ((CheckpointProcessor) wrapped).process(exchange, done -> {});

            assertThat(target.processedExchanges).hasSize(1);
        }
    }

    static class RecordingProcessor implements Processor {
        final List<Exchange> processedExchanges = new ArrayList<>();

        @Override
        public void process(Exchange exchange) {
            processedExchanges.add(exchange);
        }
    }

    static class StubRecoveryStore implements RecoveryStore {
        @Override
        public void saveCheckpoint(ExchangeSnapshot snapshot) {}

        @Override
        public List<ExchangeSnapshot> loadCheckpoints(String routeId) {
            return Collections.emptyList();
        }

        @Override
        public void saveShutdownExchange(ExchangeSnapshot snapshot) {}

        @Override
        public List<ExchangeSnapshot> loadShutdownExchanges() {
            return Collections.emptyList();
        }
    }
}
