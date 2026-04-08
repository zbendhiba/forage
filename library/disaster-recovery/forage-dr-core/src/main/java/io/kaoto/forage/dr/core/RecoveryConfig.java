package io.kaoto.forage.dr.core;

import io.kaoto.forage.core.util.config.AbstractConfig;

public class RecoveryConfig extends AbstractConfig {

    public RecoveryConfig() {
        this(null);
    }

    public RecoveryConfig(String prefix) {
        super(prefix, RecoveryConfigEntries.class);
    }

    @Override
    public String name() {
        return "forage-dr-core";
    }

    public boolean enabled() {
        return Boolean.parseBoolean(
                get(RecoveryConfigEntries.ENABLED).orElse(RecoveryConfigEntries.ENABLED.defaultValue()));
    }

    public String storageBackend() {
        return get(RecoveryConfigEntries.STORAGE_BACKEND).orElse(RecoveryConfigEntries.STORAGE_BACKEND.defaultValue());
    }

    public boolean checkpointEnabled() {
        return Boolean.parseBoolean(get(RecoveryConfigEntries.CHECKPOINT_ENABLED)
                .orElse(RecoveryConfigEntries.CHECKPOINT_ENABLED.defaultValue()));
    }

    public boolean shutdownPersistence() {
        return Boolean.parseBoolean(get(RecoveryConfigEntries.SHUTDOWN_PERSISTENCE)
                .orElse(RecoveryConfigEntries.SHUTDOWN_PERSISTENCE.defaultValue()));
    }

    public boolean replayOnStartup() {
        return Boolean.parseBoolean(get(RecoveryConfigEntries.REPLAY_ON_STARTUP)
                .orElse(RecoveryConfigEntries.REPLAY_ON_STARTUP.defaultValue()));
    }

    public boolean aiStateCheckEnabled() {
        return Boolean.parseBoolean(get(RecoveryConfigEntries.AI_STATE_CHECK_ENABLED)
                .orElse(RecoveryConfigEntries.AI_STATE_CHECK_ENABLED.defaultValue()));
    }

    public long snapshotTtlSeconds() {
        return Long.parseLong(get(RecoveryConfigEntries.SNAPSHOT_TTL_SECONDS)
                .orElse(RecoveryConfigEntries.SNAPSHOT_TTL_SECONDS.defaultValue()));
    }

    public boolean schemaAutoCreate() {
        return Boolean.parseBoolean(get(RecoveryConfigEntries.SCHEMA_AUTO_CREATE)
                .orElse(RecoveryConfigEntries.SCHEMA_AUTO_CREATE.defaultValue()));
    }

    public String jdbcDatasourceName() {
        return get(RecoveryConfigEntries.JDBC_DATASOURCE_NAME)
                .orElse(RecoveryConfigEntries.JDBC_DATASOURCE_NAME.defaultValue());
    }
}
