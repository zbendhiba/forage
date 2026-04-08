package io.kaoto.forage.dr.core;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExchangeSnapshot Tests")
class ExchangeSnapshotTest {

    @Nested
    @DisplayName("Getter/Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should set and get id")
        void shouldSetAndGetId() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            snapshot.setId(42L);
            assertThat(snapshot.getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should set and get exchange ID")
        void shouldSetAndGetExchangeId() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            snapshot.setExchangeId("ex-123");
            assertThat(snapshot.getExchangeId()).isEqualTo("ex-123");
        }

        @Test
        @DisplayName("Should set and get route ID")
        void shouldSetAndGetRouteId() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            snapshot.setRouteId("route-1");
            assertThat(snapshot.getRouteId()).isEqualTo("route-1");
        }

        @Test
        @DisplayName("Should set and get body")
        void shouldSetAndGetBody() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            byte[] body = {1, 2, 3};
            snapshot.setBody(body);
            assertThat(snapshot.getBody()).isEqualTo(body);
        }

        @Test
        @DisplayName("Should set and get headers")
        void shouldSetAndGetHeaders() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            Map<String, byte[]> headers = Map.of("key", new byte[] {1});
            snapshot.setHeaders(headers);
            assertThat(snapshot.getHeaders()).isEqualTo(headers);
        }

        @Test
        @DisplayName("Should set and get properties")
        void shouldSetAndGetProperties() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            Map<String, byte[]> props = Map.of("prop", new byte[] {2});
            snapshot.setProperties(props);
            assertThat(snapshot.getProperties()).isEqualTo(props);
        }

        @Test
        @DisplayName("Should set and get snapshot type")
        void shouldSetAndGetSnapshotType() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            snapshot.setSnapshotType(SnapshotType.SHUTDOWN);
            assertThat(snapshot.getSnapshotType()).isEqualTo(SnapshotType.SHUTDOWN);
        }

        @Test
        @DisplayName("Should set and get timestamp")
        void shouldSetAndGetTimestamp() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            snapshot.setTimestamp(1234567890L);
            assertThat(snapshot.getTimestamp()).isEqualTo(1234567890L);
        }

        @Test
        @DisplayName("Should set and get partial")
        void shouldSetAndGetPartial() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            snapshot.setPartial(true);
            assertThat(snapshot.isPartial()).isTrue();
        }

        @Test
        @DisplayName("Should default partial to false")
        void shouldDefaultPartialToFalse() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            assertThat(snapshot.isPartial()).isFalse();
        }
    }
}
