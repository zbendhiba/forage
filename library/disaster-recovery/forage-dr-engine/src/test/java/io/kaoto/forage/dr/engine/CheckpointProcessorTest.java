package io.kaoto.forage.dr.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.RecoveryStore;
import io.kaoto.forage.dr.core.SnapshotType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CheckpointProcessor Tests")
class CheckpointProcessorTest {

    private CamelContext camelContext;
    private StubRecoveryStore stubStore;
    private RecordingProcessor delegate;

    @BeforeEach
    void setUp() {
        camelContext = new DefaultCamelContext();
        stubStore = new StubRecoveryStore();
        delegate = new RecordingProcessor();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (camelContext != null) {
            camelContext.close();
        }
    }

    @Nested
    @DisplayName("Checkpoint Saving Tests")
    class CheckpointSavingTests {

        @Test
        @DisplayName("Should save checkpoint before delegating")
        void shouldSaveCheckpointBeforeDelegating() {
            CheckpointProcessor cp = new CheckpointProcessor(delegate, stubStore);
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.getIn().setBody("test");

            cp.process(exchange, done -> {});

            assertThat(stubStore.savedCheckpoints).hasSize(1);
            assertThat(stubStore.savedCheckpoints.get(0).getSnapshotType()).isEqualTo(SnapshotType.CHECKPOINT);
            assertThat(delegate.processedExchanges).hasSize(1);
        }

        @Test
        @DisplayName("Should delegate the same exchange")
        void shouldDelegateTheSameExchange() {
            CheckpointProcessor cp = new CheckpointProcessor(delegate, stubStore);
            Exchange exchange = new DefaultExchange(camelContext);

            cp.process(exchange, done -> {});

            assertThat(delegate.processedExchanges.get(0)).isSameAs(exchange);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should continue processing when checkpoint fails")
        void shouldContinueProcessingWhenCheckpointFails() {
            FailingRecoveryStore failingStore = new FailingRecoveryStore();
            CheckpointProcessor cp = new CheckpointProcessor(delegate, failingStore);
            Exchange exchange = new DefaultExchange(camelContext);

            cp.process(exchange, done -> {});

            assertThat(delegate.processedExchanges).hasSize(1);
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
        final List<ExchangeSnapshot> savedCheckpoints = new ArrayList<>();

        @Override
        public void saveCheckpoint(ExchangeSnapshot snapshot) {
            savedCheckpoints.add(snapshot);
        }

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

    static class FailingRecoveryStore implements RecoveryStore {
        @Override
        public void saveCheckpoint(ExchangeSnapshot snapshot) {
            throw new RuntimeException("Simulated checkpoint failure");
        }

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
