package io.kaoto.forage.dr.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts between live Camel {@link Exchange} objects and {@link ExchangeSnapshot} POJOs.
 * Non-serializable fields are skipped with warnings and the snapshot is marked as partial.
 */
public final class ExchangeSnapshotSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(ExchangeSnapshotSerializer.class);

    private ExchangeSnapshotSerializer() {}

    /**
     * Creates a storable snapshot from a live Camel Exchange.
     *
     * @param exchange the live exchange
     * @param snapshotType why the snapshot is being taken
     * @return the serialized snapshot
     */
    public static ExchangeSnapshot toSnapshot(Exchange exchange, SnapshotType snapshotType) {
        ExchangeSnapshot snapshot = new ExchangeSnapshot();
        snapshot.setExchangeId(exchange.getExchangeId());
        snapshot.setRouteId(exchange.getFromRouteId());
        snapshot.setSnapshotType(snapshotType);
        snapshot.setTimestamp(System.currentTimeMillis());

        boolean partial = false;

        // Serialize body
        Object body = exchange.getIn().getBody();
        byte[] serializedBody = serialize(body, "body", exchange.getExchangeId());
        if (serializedBody != null) {
            snapshot.setBody(serializedBody);
        } else if (body != null) {
            partial = true;
        }

        // Serialize headers
        Map<String, byte[]> serializedHeaders = new HashMap<>();
        for (Map.Entry<String, Object> entry : exchange.getIn().getHeaders().entrySet()) {
            byte[] value = serialize(entry.getValue(), "header '" + entry.getKey() + "'", exchange.getExchangeId());
            if (value != null) {
                serializedHeaders.put(entry.getKey(), value);
            } else if (entry.getValue() != null) {
                partial = true;
            }
        }
        snapshot.setHeaders(serializedHeaders);

        // Serialize properties
        Map<String, byte[]> serializedProperties = new HashMap<>();
        for (Map.Entry<String, Object> entry : exchange.getProperties().entrySet()) {
            byte[] value = serialize(entry.getValue(), "property '" + entry.getKey() + "'", exchange.getExchangeId());
            if (value != null) {
                serializedProperties.put(entry.getKey(), value);
            } else if (entry.getValue() != null) {
                partial = true;
            }
        }
        snapshot.setProperties(serializedProperties);

        snapshot.setPartial(partial);

        if (partial) {
            LOG.warn(
                    "Exchange {} snapshot is partial — some non-serializable fields were skipped",
                    exchange.getExchangeId());
        }

        return snapshot;
    }

    /**
     * Reconstructs a Camel Exchange from a snapshot for replay.
     *
     * @param snapshot the stored snapshot
     * @param camelContext the context to create the exchange in
     * @return the reconstructed exchange
     */
    public static Exchange toExchange(ExchangeSnapshot snapshot, CamelContext camelContext) {
        DefaultExchange exchange = new DefaultExchange(camelContext);
        exchange.setExchangeId(snapshot.getExchangeId());

        // Deserialize body
        if (snapshot.getBody() != null) {
            exchange.getIn().setBody(deserialize(snapshot.getBody(), "body", snapshot.getExchangeId()));
        }

        // Deserialize headers
        if (snapshot.getHeaders() != null) {
            for (Map.Entry<String, byte[]> entry : snapshot.getHeaders().entrySet()) {
                Object value =
                        deserialize(entry.getValue(), "header '" + entry.getKey() + "'", snapshot.getExchangeId());
                if (value != null) {
                    exchange.getIn().setHeader(entry.getKey(), value);
                }
            }
        }

        // Deserialize properties
        if (snapshot.getProperties() != null) {
            for (Map.Entry<String, byte[]> entry : snapshot.getProperties().entrySet()) {
                Object value =
                        deserialize(entry.getValue(), "property '" + entry.getKey() + "'", snapshot.getExchangeId());
                if (value != null) {
                    exchange.setProperty(entry.getKey(), value);
                }
            }
        }

        return exchange;
    }

    private static byte[] serialize(Object value, String fieldName, String exchangeId) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Serializable)) {
            LOG.warn(
                    "Exchange {} — skipping non-serializable {}: {}",
                    exchangeId,
                    fieldName,
                    value.getClass().getName());
            return null;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            oos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            LOG.warn("Exchange {} — failed to serialize {}: {}", exchangeId, fieldName, e.getMessage());
            return null;
        }
    }

    private static Object deserialize(byte[] data, String fieldName, String exchangeId) {
        if (data == null || data.length == 0) {
            return null;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        } catch (Exception e) {
            LOG.warn("Exchange {} — failed to deserialize {}: {}", exchangeId, fieldName, e.getMessage());
            return null;
        }
    }
}
