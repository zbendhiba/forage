package io.kaoto.forage.dr.core;

import io.kaoto.forage.core.util.config.ConfigEntries;
import io.kaoto.forage.core.util.config.ConfigModule;
import io.kaoto.forage.core.util.config.ConfigTag;

public final class RecoveryConfigEntries extends ConfigEntries {

    public static final ConfigModule ENABLED = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.enabled",
            "Master switch for disaster recovery",
            "Enabled",
            "true",
            "boolean",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule STORAGE_BACKEND = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.storage.backend",
            "Storage backend for persisting exchange snapshots",
            "Storage Backend",
            "jdbc",
            "string",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule CHECKPOINT_ENABLED = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.checkpoint.enabled",
            "Enable checkpoint/resume for routes",
            "Checkpoint Enabled",
            "true",
            "boolean",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule SHUTDOWN_PERSISTENCE = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.shutdown.persistence",
            "Enable graceful shutdown exchange persistence",
            "Shutdown Persistence",
            "true",
            "boolean",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule REPLAY_ON_STARTUP = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.replay.on.startup",
            "Replay persisted exchanges on startup",
            "Replay On Startup",
            "true",
            "boolean",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule AI_STATE_CHECK_ENABLED = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.ai.state.check.enabled",
            "Warn about in-memory AI providers on startup",
            "AI State Check",
            "true",
            "boolean",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule SNAPSHOT_TTL_SECONDS = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.snapshot.ttl.seconds",
            "Time-to-live for snapshots in seconds. Expired snapshots are purged on load.",
            "Snapshot TTL (seconds)",
            "86400",
            "integer",
            false,
            ConfigTag.COMMON);

    public static final ConfigModule SCHEMA_AUTO_CREATE = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.schema.auto.create",
            "Auto-create database schema on startup",
            "Schema Auto Create",
            "true",
            "boolean",
            false,
            ConfigTag.ADVANCED);

    public static final ConfigModule JDBC_DATASOURCE_NAME = ConfigModule.of(
            RecoveryConfig.class,
            "forage.dr.jdbc.datasource.name",
            "Registry name of the DataSource to use for JDBC storage",
            "JDBC DataSource Name",
            "dataSource",
            "string",
            false,
            ConfigTag.ADVANCED);

    static {
        initModules(
                RecoveryConfigEntries.class,
                ENABLED,
                STORAGE_BACKEND,
                CHECKPOINT_ENABLED,
                SHUTDOWN_PERSISTENCE,
                REPLAY_ON_STARTUP,
                AI_STATE_CHECK_ENABLED,
                SNAPSHOT_TTL_SECONDS,
                SCHEMA_AUTO_CREATE,
                JDBC_DATASOURCE_NAME);
    }
}
