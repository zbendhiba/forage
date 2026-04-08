package io.kaoto.forage.dr.core;

import java.util.Map;

/**
 * Serializable representation of a Camel Exchange at a point in time.
 * Camel's Exchange is a runtime object tied to the CamelContext and cannot
 * be serialized directly — this POJO is the portable, storable form.
 */
public class ExchangeSnapshot {

    private long id;
    private String exchangeId;
    private String routeId;
    private byte[] body;
    private Map<String, byte[]> headers;
    private Map<String, byte[]> properties;
    private SnapshotType snapshotType;
    private long timestamp;
    private boolean partial;

    public ExchangeSnapshot() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public Map<String, byte[]> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, byte[]> headers) {
        this.headers = headers;
    }

    public Map<String, byte[]> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, byte[]> properties) {
        this.properties = properties;
    }

    public SnapshotType getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(SnapshotType snapshotType) {
        this.snapshotType = snapshotType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }
}
