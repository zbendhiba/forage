package io.kaoto.forage.dr.store.jdbc;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.SnapshotType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JdbcRecoveryStore Tests")
class JdbcRecoveryStoreTest {

    private DataSource dataSource;
    private JdbcRecoveryStore store;

    @BeforeEach
    void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:store_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        SchemaInitializer.initialize(dataSource);
        store = new JdbcRecoveryStore(dataSource, 86400);
    }

    private ExchangeSnapshot createTestSnapshot(String exchangeId, String routeId, SnapshotType type) {
        ExchangeSnapshot snapshot = new ExchangeSnapshot();
        snapshot.setExchangeId(exchangeId);
        snapshot.setRouteId(routeId);
        snapshot.setSnapshotType(type);
        snapshot.setTimestamp(System.currentTimeMillis());
        snapshot.setBody("test-body".getBytes());

        Map<String, byte[]> headers = new HashMap<>();
        headers.put("h1", "val1".getBytes());
        snapshot.setHeaders(headers);

        Map<String, byte[]> properties = new HashMap<>();
        properties.put("p1", "pval1".getBytes());
        snapshot.setProperties(properties);

        snapshot.setPartial(false);
        return snapshot;
    }

    @Nested
    @DisplayName("Checkpoint Tests")
    class CheckpointTests {

        @Test
        @DisplayName("Should save and load checkpoint")
        void shouldSaveAndLoadCheckpoint() {
            ExchangeSnapshot snapshot = createTestSnapshot("ex-1", "route1", SnapshotType.CHECKPOINT);

            store.saveCheckpoint(snapshot);
            List<ExchangeSnapshot> loaded = store.loadCheckpoints("route1");

            assertThat(loaded).hasSize(1);
            assertThat(loaded.get(0).getExchangeId()).isEqualTo("ex-1");
            assertThat(loaded.get(0).getRouteId()).isEqualTo("route1");
            assertThat(loaded.get(0).getSnapshotType()).isEqualTo(SnapshotType.CHECKPOINT);
            assertThat(loaded.get(0).isPartial()).isFalse();
        }

        @Test
        @DisplayName("Should load checkpoints filtered by route ID")
        void shouldLoadCheckpointsFilteredByRouteId() {
            store.saveCheckpoint(createTestSnapshot("ex-1", "route1", SnapshotType.CHECKPOINT));
            store.saveCheckpoint(createTestSnapshot("ex-2", "route2", SnapshotType.CHECKPOINT));

            List<ExchangeSnapshot> loaded = store.loadCheckpoints("route1");

            assertThat(loaded).hasSize(1);
            assertThat(loaded.get(0).getExchangeId()).isEqualTo("ex-1");
        }

        @Test
        @DisplayName("Should return empty list when no checkpoints exist")
        void shouldReturnEmptyListWhenNoCheckpoints() {
            List<ExchangeSnapshot> loaded = store.loadCheckpoints("nonexistent");

            assertThat(loaded).isEmpty();
        }

        @Test
        @DisplayName("Should load multiple checkpoints ordered by created_at")
        void shouldLoadMultipleCheckpoints() {
            for (int i = 1; i <= 3; i++) {
                ExchangeSnapshot snapshot = createTestSnapshot("ex-" + i, "route1", SnapshotType.CHECKPOINT);
                snapshot.setTimestamp(System.currentTimeMillis() + i);
                store.saveCheckpoint(snapshot);
            }

            List<ExchangeSnapshot> loaded = store.loadCheckpoints("route1");

            assertThat(loaded).hasSize(3);
            assertThat(loaded.get(0).getExchangeId()).isEqualTo("ex-1");
            assertThat(loaded.get(2).getExchangeId()).isEqualTo("ex-3");
        }

        @Test
        @DisplayName("Should round-trip body and headers")
        void shouldRoundTripBodyAndHeaders() {
            ExchangeSnapshot snapshot = createTestSnapshot("ex-1", "route1", SnapshotType.CHECKPOINT);

            store.saveCheckpoint(snapshot);
            List<ExchangeSnapshot> loaded = store.loadCheckpoints("route1");

            assertThat(loaded).hasSize(1);
            assertThat(loaded.get(0).getBody()).isEqualTo("test-body".getBytes());
            assertThat(loaded.get(0).getHeaders()).isNotNull();
            assertThat(loaded.get(0).getProperties()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Shutdown Exchange Tests")
    class ShutdownExchangeTests {

        @Test
        @DisplayName("Should save and load shutdown exchange")
        void shouldSaveAndLoadShutdownExchange() {
            ExchangeSnapshot snapshot = createTestSnapshot("ex-1", "route1", SnapshotType.SHUTDOWN);

            store.saveShutdownExchange(snapshot);
            List<ExchangeSnapshot> loaded = store.loadShutdownExchanges();

            assertThat(loaded).hasSize(1);
            assertThat(loaded.get(0).getExchangeId()).isEqualTo("ex-1");
            assertThat(loaded.get(0).getSnapshotType()).isEqualTo(SnapshotType.SHUTDOWN);
        }

        @Test
        @DisplayName("Should return empty list when no shutdown exchanges exist")
        void shouldReturnEmptyListWhenNoShutdownExchanges() {
            List<ExchangeSnapshot> loaded = store.loadShutdownExchanges();

            assertThat(loaded).isEmpty();
        }

        @Test
        @DisplayName("Should load multiple shutdown exchanges")
        void shouldLoadMultipleShutdownExchanges() {
            store.saveShutdownExchange(createTestSnapshot("ex-1", "route1", SnapshotType.SHUTDOWN));
            store.saveShutdownExchange(createTestSnapshot("ex-2", "route2", SnapshotType.SHUTDOWN));

            List<ExchangeSnapshot> loaded = store.loadShutdownExchanges();

            assertThat(loaded).hasSize(2);
        }
    }

    @Nested
    @DisplayName("TTL Expiry Tests")
    class TtlExpiryTests {

        @Test
        @DisplayName("Should purge expired checkpoints on load")
        void shouldPurgeExpiredCheckpoints() {
            JdbcRecoveryStore shortTtlStore = new JdbcRecoveryStore(dataSource, 1);

            ExchangeSnapshot snapshot = createTestSnapshot("ex-old", "route1", SnapshotType.CHECKPOINT);
            snapshot.setTimestamp(System.currentTimeMillis() - 2000);
            shortTtlStore.saveCheckpoint(snapshot);

            List<ExchangeSnapshot> loaded = shortTtlStore.loadCheckpoints("route1");

            assertThat(loaded).isEmpty();
        }

        @Test
        @DisplayName("Should not purge non-expired checkpoints")
        void shouldNotPurgeNonExpiredCheckpoints() {
            ExchangeSnapshot snapshot = createTestSnapshot("ex-fresh", "route1", SnapshotType.CHECKPOINT);

            store.saveCheckpoint(snapshot);
            List<ExchangeSnapshot> loaded = store.loadCheckpoints("route1");

            assertThat(loaded).hasSize(1);
        }

        @Test
        @DisplayName("Should purge expired shutdown exchanges on load")
        void shouldPurgeExpiredShutdownExchanges() {
            JdbcRecoveryStore shortTtlStore = new JdbcRecoveryStore(dataSource, 1);

            ExchangeSnapshot snapshot = createTestSnapshot("ex-old", "route1", SnapshotType.SHUTDOWN);
            snapshot.setTimestamp(System.currentTimeMillis() - 2000);
            shortTtlStore.saveShutdownExchange(snapshot);

            List<ExchangeSnapshot> loaded = shortTtlStore.loadShutdownExchanges();

            assertThat(loaded).isEmpty();
        }

        @Test
        @DisplayName("Should keep non-expired and purge expired checkpoints together")
        void shouldKeepNonExpiredAndPurgeExpired() {
            JdbcRecoveryStore shortTtlStore = new JdbcRecoveryStore(dataSource, 1);

            ExchangeSnapshot expired = createTestSnapshot("ex-old", "route1", SnapshotType.CHECKPOINT);
            expired.setTimestamp(System.currentTimeMillis() - 2000);
            shortTtlStore.saveCheckpoint(expired);

            ExchangeSnapshot fresh = createTestSnapshot("ex-fresh", "route1", SnapshotType.CHECKPOINT);
            shortTtlStore.saveCheckpoint(fresh);

            List<ExchangeSnapshot> loaded = shortTtlStore.loadCheckpoints("route1");

            assertThat(loaded).hasSize(1);
            assertThat(loaded.get(0).getExchangeId()).isEqualTo("ex-fresh");
        }
    }

    @Nested
    @DisplayName("Partial Snapshot Tests")
    class PartialSnapshotTests {

        @Test
        @DisplayName("Should store and retrieve partial snapshot")
        void shouldStoreAndRetrievePartialSnapshot() {
            ExchangeSnapshot snapshot = createTestSnapshot("ex-1", "route1", SnapshotType.CHECKPOINT);
            snapshot.setPartial(true);
            snapshot.setBody(null);

            store.saveCheckpoint(snapshot);
            List<ExchangeSnapshot> loaded = store.loadCheckpoints("route1");

            assertThat(loaded).hasSize(1);
            assertThat(loaded.get(0).isPartial()).isTrue();
            assertThat(loaded.get(0).getBody()).isNull();
        }
    }
}
