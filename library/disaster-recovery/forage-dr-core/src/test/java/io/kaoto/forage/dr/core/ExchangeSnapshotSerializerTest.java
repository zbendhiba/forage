package io.kaoto.forage.dr.core;

import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExchangeSnapshotSerializer Tests")
class ExchangeSnapshotSerializerTest {

    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        camelContext = new DefaultCamelContext();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (camelContext != null) {
            camelContext.close();
        }
    }

    @Nested
    @DisplayName("toSnapshot Tests")
    class ToSnapshotTests {

        @Test
        @DisplayName("Should serialize string body")
        void shouldSerializeStringBody() {
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.getIn().setBody("hello");

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT);

            assertThat(snapshot.getBody()).isNotNull();
            assertThat(snapshot.getExchangeId()).isEqualTo(exchange.getExchangeId());
            assertThat(snapshot.getSnapshotType()).isEqualTo(SnapshotType.CHECKPOINT);
            assertThat(snapshot.isPartial()).isFalse();
        }

        @Test
        @DisplayName("Should handle null body")
        void shouldHandleNullBody() {
            Exchange exchange = new DefaultExchange(camelContext);

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT);

            assertThat(snapshot.getBody()).isNull();
            assertThat(snapshot.isPartial()).isFalse();
        }

        @Test
        @DisplayName("Should serialize headers")
        void shouldSerializeHeaders() {
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.getIn().setHeader("key1", "val1");
            exchange.getIn().setHeader("key2", 42);

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT);

            assertThat(snapshot.getHeaders()).hasSize(2);
            assertThat(snapshot.getHeaders().get("key1")).isNotNull();
            assertThat(snapshot.getHeaders().get("key2")).isNotNull();
        }

        @Test
        @DisplayName("Should serialize properties")
        void shouldSerializeProperties() {
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty("prop1", "propVal");

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT);

            assertThat(snapshot.getProperties()).containsKey("prop1");
        }

        @Test
        @DisplayName("Should mark partial for non-serializable body")
        void shouldMarkPartialForNonSerializableBody() {
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.getIn().setBody(new Object());

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT);

            assertThat(snapshot.isPartial()).isTrue();
            assertThat(snapshot.getBody()).isNull();
        }

        @Test
        @DisplayName("Should mark partial for non-serializable header")
        void shouldMarkPartialForNonSerializableHeader() {
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.getIn().setHeader("good", "ok");
            exchange.getIn().setHeader("bad", new Object());

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT);

            assertThat(snapshot.isPartial()).isTrue();
            assertThat(snapshot.getHeaders()).containsKey("good");
            assertThat(snapshot.getHeaders()).doesNotContainKey("bad");
        }

        @Test
        @DisplayName("Should handle empty headers")
        void shouldHandleEmptyHeaders() {
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.getIn().setBody("test");

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT);

            assertThat(snapshot.getHeaders()).isEmpty();
        }

        @Test
        @DisplayName("Should set timestamp")
        void shouldSetTimestamp() {
            Exchange exchange = new DefaultExchange(camelContext);

            long before = System.currentTimeMillis();
            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.CHECKPOINT);
            long after = System.currentTimeMillis();

            assertThat(snapshot.getTimestamp()).isBetween(before, after);
        }

        @Test
        @DisplayName("Should set snapshot type SHUTDOWN")
        void shouldSetSnapshotTypeShutdown() {
            Exchange exchange = new DefaultExchange(camelContext);

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(exchange, SnapshotType.SHUTDOWN);

            assertThat(snapshot.getSnapshotType()).isEqualTo(SnapshotType.SHUTDOWN);
        }
    }

    @Nested
    @DisplayName("toExchange Tests")
    class ToExchangeTests {

        @Test
        @DisplayName("Should reconstruct body from snapshot")
        void shouldReconstructBodyFromSnapshot() {
            Exchange original = new DefaultExchange(camelContext);
            original.getIn().setBody("test data");
            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.CHECKPOINT);

            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getIn().getBody(String.class)).isEqualTo("test data");
        }

        @Test
        @DisplayName("Should reconstruct headers from snapshot")
        void shouldReconstructHeadersFromSnapshot() {
            Exchange original = new DefaultExchange(camelContext);
            original.getIn().setHeader("myHeader", "myValue");
            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.CHECKPOINT);

            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getIn().getHeader("myHeader", String.class))
                    .isEqualTo("myValue");
        }

        @Test
        @DisplayName("Should reconstruct properties from snapshot")
        void shouldReconstructPropertiesFromSnapshot() {
            Exchange original = new DefaultExchange(camelContext);
            original.setProperty("myProp", "myPropVal");
            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.CHECKPOINT);

            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getProperty("myProp", String.class)).isEqualTo("myPropVal");
        }

        @Test
        @DisplayName("Should set exchange ID from snapshot")
        void shouldSetExchangeIdFromSnapshot() {
            Exchange original = new DefaultExchange(camelContext);
            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.CHECKPOINT);

            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getExchangeId()).isEqualTo(original.getExchangeId());
        }

        @Test
        @DisplayName("Should handle null body in snapshot")
        void shouldHandleNullBodyInSnapshot() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            snapshot.setExchangeId("test-id");

            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getIn().getBody()).isNull();
        }

        @Test
        @DisplayName("Should handle null headers in snapshot")
        void shouldHandleNullHeadersInSnapshot() {
            ExchangeSnapshot snapshot = new ExchangeSnapshot();
            snapshot.setExchangeId("test-id");

            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getIn().getHeaders()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Round-Trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Should round-trip string body")
        void shouldRoundTripStringBody() {
            Exchange original = new DefaultExchange(camelContext);
            original.getIn().setBody("hello world");

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.CHECKPOINT);
            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getIn().getBody(String.class)).isEqualTo("hello world");
        }

        @Test
        @DisplayName("Should round-trip with headers and properties")
        void shouldRoundTripWithHeadersAndProperties() {
            Exchange original = new DefaultExchange(camelContext);
            original.getIn().setBody("payload");
            original.getIn().setHeader("stringHeader", "strVal");
            original.getIn().setHeader("intHeader", 42);
            original.getIn().setHeader("longHeader", 100L);
            original.setProperty("prop1", "propVal1");
            original.setProperty("prop2", "propVal2");

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.SHUTDOWN);
            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getIn().getBody(String.class)).isEqualTo("payload");
            assertThat(reconstructed.getIn().getHeader("stringHeader", String.class))
                    .isEqualTo("strVal");
            assertThat(reconstructed.getIn().getHeader("intHeader", Integer.class))
                    .isEqualTo(42);
            assertThat(reconstructed.getIn().getHeader("longHeader", Long.class))
                    .isEqualTo(100L);
            assertThat(reconstructed.getProperty("prop1", String.class)).isEqualTo("propVal1");
            assertThat(reconstructed.getProperty("prop2", String.class)).isEqualTo("propVal2");
        }

        @Test
        @DisplayName("Should round-trip empty exchange")
        void shouldRoundTripEmptyExchange() {
            Exchange original = new DefaultExchange(camelContext);

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.CHECKPOINT);
            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getIn().getBody()).isNull();
            assertThat(reconstructed.getIn().getHeaders()).isEmpty();
        }

        @Test
        @DisplayName("Should round-trip byte array body")
        void shouldRoundTripByteArrayBody() {
            byte[] data = {1, 2, 3, 4, 5};
            Exchange original = new DefaultExchange(camelContext);
            original.getIn().setBody(data);

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.CHECKPOINT);
            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            assertThat(reconstructed.getIn().getBody(byte[].class)).isEqualTo(data);
        }

        @Test
        @DisplayName("Should round-trip map body")
        void shouldRoundTripMapBody() {
            Exchange original = new DefaultExchange(camelContext);
            original.getIn().setBody(Map.of("key", "value", "num", 123));

            ExchangeSnapshot snapshot = ExchangeSnapshotSerializer.toSnapshot(original, SnapshotType.CHECKPOINT);
            Exchange reconstructed = ExchangeSnapshotSerializer.toExchange(snapshot, camelContext);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = reconstructed.getIn().getBody(Map.class);
            assertThat(body).containsEntry("key", "value").containsEntry("num", 123);
        }
    }
}
