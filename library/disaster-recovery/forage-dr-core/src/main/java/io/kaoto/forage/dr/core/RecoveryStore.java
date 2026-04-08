package io.kaoto.forage.dr.core;

import java.util.List;

/**
 * Storage SPI for disaster recovery. Backends (JDBC, Redis, Infinispan)
 * implement this interface to persist and retrieve exchange snapshots.
 *
 * <p>Snapshots expire based on a configurable TTL ({@code forage.dr.snapshot.ttl}).
 * Expired snapshots are purged on load — no explicit delete operations needed.
 * For Redis/Infinispan backends, native TTL expiry can be used instead.</p>
 */
public interface RecoveryStore {

    /**
     * Persists an exchange snapshot as a checkpoint.
     *
     * @param snapshot the exchange state to persist
     */
    void saveCheckpoint(ExchangeSnapshot snapshot);

    /**
     * Loads non-expired checkpoints for a given route.
     * Implementations should purge expired snapshots before returning.
     *
     * @param routeId the Camel route ID
     * @return list of checkpointed snapshots within TTL, empty if none
     */
    List<ExchangeSnapshot> loadCheckpoints(String routeId);

    /**
     * Persists an in-flight exchange during graceful shutdown.
     *
     * @param snapshot the exchange state to persist
     */
    void saveShutdownExchange(ExchangeSnapshot snapshot);

    /**
     * Loads non-expired exchanges that were drained during a previous shutdown.
     * Implementations should purge expired snapshots before returning.
     *
     * @return list of shutdown-drained snapshots within TTL, empty if none
     */
    List<ExchangeSnapshot> loadShutdownExchanges();
}
