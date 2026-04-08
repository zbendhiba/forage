package io.kaoto.forage.dr.store.jdbc;

import javax.sql.DataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.RecoveryStore;
import io.kaoto.forage.dr.core.SnapshotType;

/**
 * JDBC-backed implementation of {@link RecoveryStore}.
 * Uses two tables: {@code forage_dr_checkpoint} and {@code forage_dr_shutdown_exchange}.
 *
 * <p>Expired snapshots are purged on load based on the configured TTL.</p>
 */
public class JdbcRecoveryStore implements RecoveryStore {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcRecoveryStore.class);

    private static final String INSERT_CHECKPOINT =
            "INSERT INTO forage_dr_checkpoint (exchange_id, route_id, body, headers, properties, snapshot_type, created_at, partial) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_CHECKPOINTS_BY_ROUTE =
            "SELECT id, exchange_id, route_id, body, headers, properties, snapshot_type, created_at, partial "
                    + "FROM forage_dr_checkpoint WHERE route_id = ? AND created_at > ? ORDER BY created_at ASC";

    private static final String PURGE_EXPIRED_CHECKPOINTS = "DELETE FROM forage_dr_checkpoint WHERE created_at <= ?";

    private static final String INSERT_SHUTDOWN_EXCHANGE =
            "INSERT INTO forage_dr_shutdown_exchange (exchange_id, route_id, body, headers, properties, snapshot_type, created_at, partial) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_SHUTDOWN_EXCHANGES =
            "SELECT id, exchange_id, route_id, body, headers, properties, snapshot_type, created_at, partial "
                    + "FROM forage_dr_shutdown_exchange WHERE created_at > ? ORDER BY created_at ASC";

    private static final String PURGE_EXPIRED_SHUTDOWN_EXCHANGES =
            "DELETE FROM forage_dr_shutdown_exchange WHERE created_at <= ?";

    private final DataSource dataSource;
    private final long ttlSeconds;

    public JdbcRecoveryStore(DataSource dataSource, long ttlSeconds) {
        this.dataSource = dataSource;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public void saveCheckpoint(ExchangeSnapshot snapshot) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(INSERT_CHECKPOINT)) {
            setSnapshotParameters(ps, snapshot);
            ps.executeUpdate();
            LOG.debug("Saved checkpoint for exchange {}", snapshot.getExchangeId());
        } catch (SQLException e) {
            LOG.error("Failed to save checkpoint for exchange {}: {}", snapshot.getExchangeId(), e.getMessage(), e);
        }
    }

    @Override
    public List<ExchangeSnapshot> loadCheckpoints(String routeId) {
        purgeExpired(PURGE_EXPIRED_CHECKPOINTS, "checkpoints");

        List<ExchangeSnapshot> snapshots = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_CHECKPOINTS_BY_ROUTE)) {
            ps.setString(1, routeId);
            ps.setLong(2, cutoffTimestamp());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    snapshots.add(readSnapshot(rs));
                }
            }
            LOG.debug("Loaded {} checkpoints for route {}", snapshots.size(), routeId);
        } catch (SQLException e) {
            LOG.error("Failed to load checkpoints for route {}: {}", routeId, e.getMessage(), e);
        }
        return snapshots;
    }

    @Override
    public void saveShutdownExchange(ExchangeSnapshot snapshot) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(INSERT_SHUTDOWN_EXCHANGE)) {
            setSnapshotParameters(ps, snapshot);
            ps.executeUpdate();
            LOG.debug("Saved shutdown exchange {}", snapshot.getExchangeId());
        } catch (SQLException e) {
            LOG.error("Failed to save shutdown exchange {}: {}", snapshot.getExchangeId(), e.getMessage(), e);
        }
    }

    @Override
    public List<ExchangeSnapshot> loadShutdownExchanges() {
        purgeExpired(PURGE_EXPIRED_SHUTDOWN_EXCHANGES, "shutdown exchanges");

        List<ExchangeSnapshot> snapshots = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(SELECT_SHUTDOWN_EXCHANGES)) {
            ps.setLong(1, cutoffTimestamp());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    snapshots.add(readSnapshot(rs));
                }
            }
            LOG.debug("Loaded {} shutdown exchanges", snapshots.size());
        } catch (SQLException e) {
            LOG.error("Failed to load shutdown exchanges: {}", e.getMessage(), e);
        }
        return snapshots;
    }

    private void purgeExpired(String purgeQuery, String description) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(purgeQuery)) {
            ps.setLong(1, cutoffTimestamp());
            int purged = ps.executeUpdate();
            if (purged > 0) {
                LOG.info("Purged {} expired {}", purged, description);
            }
        } catch (SQLException e) {
            LOG.warn("Failed to purge expired {}: {}", description, e.getMessage());
        }
    }

    private long cutoffTimestamp() {
        return System.currentTimeMillis() - (ttlSeconds * 1000);
    }

    private void setSnapshotParameters(PreparedStatement ps, ExchangeSnapshot snapshot) throws SQLException {
        ps.setString(1, snapshot.getExchangeId());
        ps.setString(2, snapshot.getRouteId());
        ps.setBytes(3, snapshot.getBody());
        ps.setBytes(4, serializeMap(snapshot.getHeaders()));
        ps.setBytes(5, serializeMap(snapshot.getProperties()));
        ps.setString(6, snapshot.getSnapshotType().name());
        ps.setLong(7, snapshot.getTimestamp());
        ps.setBoolean(8, snapshot.isPartial());
    }

    private ExchangeSnapshot readSnapshot(ResultSet rs) throws SQLException {
        ExchangeSnapshot snapshot = new ExchangeSnapshot();
        snapshot.setId(rs.getLong("id"));
        snapshot.setExchangeId(rs.getString("exchange_id"));
        snapshot.setRouteId(rs.getString("route_id"));
        snapshot.setBody(rs.getBytes("body"));
        snapshot.setHeaders(deserializeMap(rs.getBytes("headers")));
        snapshot.setProperties(deserializeMap(rs.getBytes("properties")));
        snapshot.setSnapshotType(SnapshotType.valueOf(rs.getString("snapshot_type")));
        snapshot.setTimestamp(rs.getLong("created_at"));
        snapshot.setPartial(rs.getBoolean("partial"));
        return snapshot;
    }

    private byte[] serializeMap(Map<String, byte[]> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(map);
            oos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            LOG.warn("Failed to serialize map: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, byte[]> deserializeMap(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Map<String, byte[]>) ois.readObject();
        } catch (Exception e) {
            LOG.warn("Failed to deserialize map: {}", e.getMessage());
            return null;
        }
    }
}
