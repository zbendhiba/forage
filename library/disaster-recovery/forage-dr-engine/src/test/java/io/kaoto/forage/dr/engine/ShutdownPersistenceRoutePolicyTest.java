package io.kaoto.forage.dr.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.RecoveryStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShutdownPersistenceRoutePolicy Tests")
class ShutdownPersistenceRoutePolicyTest {

    private CamelContext camelContext;
    private StubRecoveryStore stubStore;
    private ShutdownPersistenceRoutePolicy policy;
    private Route testRoute;

    @BeforeEach
    void setUp() throws Exception {
        camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:test").routeId("test-route").to("log:test");
            }
        });
        camelContext.start();
        testRoute = camelContext.getRoute("test-route");

        stubStore = new StubRecoveryStore();
        policy = new ShutdownPersistenceRoutePolicy(stubStore);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (camelContext != null) {
            camelContext.close();
        }
    }

    @Nested
    @DisplayName("Exchange Tracking Tests")
    class ExchangeTrackingTests {

        @Test
        @DisplayName("Should track exchange on begin and drain on stop")
        void shouldTrackExchangeOnBeginAndDrainOnStop() {
            Exchange exchange = new DefaultExchange(camelContext);

            policy.onExchangeBegin(testRoute, exchange);
            policy.onStop(testRoute);

            assertThat(stubStore.savedShutdownExchanges).hasSize(1);
        }

        @Test
        @DisplayName("Should remove exchange on done — nothing drained on stop")
        void shouldRemoveExchangeOnDone() {
            Exchange exchange = new DefaultExchange(camelContext);

            policy.onExchangeBegin(testRoute, exchange);
            policy.onExchangeDone(testRoute, exchange);
            policy.onStop(testRoute);

            assertThat(stubStore.savedShutdownExchanges).isEmpty();
        }
    }

    @Nested
    @DisplayName("Shutdown Drain Tests")
    class ShutdownDrainTests {

        @Test
        @DisplayName("Should drain all in-flight exchanges to store")
        void shouldDrainInflightExchangesToStore() {
            for (int i = 0; i < 3; i++) {
                policy.onExchangeBegin(testRoute, new DefaultExchange(camelContext));
            }

            policy.onStop(testRoute);

            assertThat(stubStore.savedShutdownExchanges).hasSize(3);
        }

        @Test
        @DisplayName("Should save nothing when no in-flight exchanges")
        void shouldSaveNothingWhenNoInflightExchanges() {
            policy.onStop(testRoute);

            assertThat(stubStore.savedShutdownExchanges).isEmpty();
        }

        @Test
        @DisplayName("Should continue saving when one save fails")
        void shouldContinueWhenOneSaveFails() {
            PartiallyFailingRecoveryStore failingStore = new PartiallyFailingRecoveryStore();
            ShutdownPersistenceRoutePolicy failPolicy = new ShutdownPersistenceRoutePolicy(failingStore);

            for (int i = 0; i < 3; i++) {
                failPolicy.onExchangeBegin(testRoute, new DefaultExchange(camelContext));
            }

            failPolicy.onStop(testRoute);

            assertThat(failingStore.savedShutdownExchanges).hasSize(2);
        }
    }

    static class StubRecoveryStore implements RecoveryStore {
        final List<ExchangeSnapshot> savedCheckpoints = new ArrayList<>();
        final List<ExchangeSnapshot> savedShutdownExchanges = new ArrayList<>();

        @Override
        public void saveCheckpoint(ExchangeSnapshot snapshot) {
            savedCheckpoints.add(snapshot);
        }

        @Override
        public List<ExchangeSnapshot> loadCheckpoints(String routeId) {
            return Collections.emptyList();
        }

        @Override
        public void saveShutdownExchange(ExchangeSnapshot snapshot) {
            savedShutdownExchanges.add(snapshot);
        }

        @Override
        public List<ExchangeSnapshot> loadShutdownExchanges() {
            return Collections.emptyList();
        }
    }

    static class PartiallyFailingRecoveryStore implements RecoveryStore {
        int saveCount = 0;
        final List<ExchangeSnapshot> savedShutdownExchanges = new ArrayList<>();

        @Override
        public void saveCheckpoint(ExchangeSnapshot snapshot) {}

        @Override
        public List<ExchangeSnapshot> loadCheckpoints(String routeId) {
            return Collections.emptyList();
        }

        @Override
        public void saveShutdownExchange(ExchangeSnapshot snapshot) {
            if (saveCount++ == 0) {
                throw new RuntimeException("Simulated failure");
            }
            savedShutdownExchanges.add(snapshot);
        }

        @Override
        public List<ExchangeSnapshot> loadShutdownExchanges() {
            return Collections.emptyList();
        }
    }
}
