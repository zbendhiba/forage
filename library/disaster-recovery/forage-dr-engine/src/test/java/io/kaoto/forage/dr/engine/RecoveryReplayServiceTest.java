package io.kaoto.forage.dr.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.ExchangeSnapshotSerializer;
import io.kaoto.forage.dr.core.RecoveryStore;
import io.kaoto.forage.dr.core.SnapshotType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("RecoveryReplayService Tests")
class RecoveryReplayServiceTest {

    private CamelContext camelContext;

    @BeforeEach
    void setUp() throws Exception {
        camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("seda:route1").routeId("route1").to("log:output1");
                from("seda:route2").routeId("route2").to("log:output2");
            }
        });
        camelContext.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (camelContext != null) {
            camelContext.close();
        }
    }

    private ExchangeSnapshot createSnapshotForRoute(String routeId, SnapshotType type) {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody("replayed-body");
        ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, type);
        snapshot.setRouteId(routeId);
        return snapshot;
    }

    @Nested
    @DisplayName("Replay Tests")
    class ReplayTests {

        @Test
        @DisplayName("Should replay shutdown exchanges")
        void shouldReplayShutdownExchanges() {
            PreloadedRecoveryStore store = new PreloadedRecoveryStore();
            store.shutdownExchanges.add(createSnapshotForRoute("route1", SnapshotType.SHUTDOWN));

            RecoveryReplayService service = new RecoveryReplayService(store);

            assertThatCode(() -> service.replay(camelContext)).doesNotThrowAnyException();
            assertThat(store.shutdownExchangesLoaded).isTrue();
        }

        @Test
        @DisplayName("Should replay checkpoints per route")
        void shouldReplayCheckpointsPerRoute() {
            PreloadedRecoveryStore store = new PreloadedRecoveryStore();
            store.checkpointsByRoute.put("route1", List.of(createSnapshotForRoute("route1", SnapshotType.CHECKPOINT)));

            RecoveryReplayService service = new RecoveryReplayService(store);

            assertThatCode(() -> service.replay(camelContext)).doesNotThrowAnyException();
            assertThat(store.checkpointsLoadedForRoutes).contains("route1");
        }

        @Test
        @DisplayName("Should skip snapshots for non-existent routes")
        void shouldSkipSnapshotsForNonExistentRoutes() {
            PreloadedRecoveryStore store = new PreloadedRecoveryStore();
            store.shutdownExchanges.add(createSnapshotForRoute("nonexistent", SnapshotType.SHUTDOWN));

            RecoveryReplayService service = new RecoveryReplayService(store);

            assertThatCode(() -> service.replay(camelContext)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle empty store")
        void shouldHandleEmptyStore() {
            PreloadedRecoveryStore store = new PreloadedRecoveryStore();

            RecoveryReplayService service = new RecoveryReplayService(store);

            assertThatCode(() -> service.replay(camelContext)).doesNotThrowAnyException();
        }
    }

    static class PreloadedRecoveryStore implements RecoveryStore {
        final List<ExchangeSnapshot> shutdownExchanges = new ArrayList<>();
        final Map<String, List<ExchangeSnapshot>> checkpointsByRoute = new HashMap<>();
        boolean shutdownExchangesLoaded = false;
        final List<String> checkpointsLoadedForRoutes = new ArrayList<>();

        @Override
        public void saveCheckpoint(ExchangeSnapshot snapshot) {}

        @Override
        public List<ExchangeSnapshot> loadCheckpoints(String routeId) {
            checkpointsLoadedForRoutes.add(routeId);
            return checkpointsByRoute.getOrDefault(routeId, Collections.emptyList());
        }

        @Override
        public void saveShutdownExchange(ExchangeSnapshot snapshot) {}

        @Override
        public List<ExchangeSnapshot> loadShutdownExchanges() {
            shutdownExchangesLoaded = true;
            return shutdownExchanges;
        }
    }
}
