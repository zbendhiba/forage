package io.kaoto.forage.dr.core;

import io.kaoto.forage.core.util.config.ConfigStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecoveryConfig Tests")
class RecoveryConfigTest {

    @BeforeEach
    void clearConfigCache() {
        ConfigStore.getInstance().reload();
        clearDrSystemProperties();
    }

    @AfterEach
    void cleanUp() {
        clearDrSystemProperties();
        ConfigStore.getInstance().reload();
    }

    private void clearDrSystemProperties() {
        System.clearProperty("forage.dr.enabled");
        System.clearProperty("forage.dr.storage.backend");
        System.clearProperty("forage.dr.checkpoint.enabled");
        System.clearProperty("forage.dr.shutdown.persistence");
        System.clearProperty("forage.dr.replay.on.startup");
        System.clearProperty("forage.dr.ai.state.check.enabled");
        System.clearProperty("forage.dr.snapshot.ttl.seconds");
        System.clearProperty("forage.dr.schema.auto.create");
        System.clearProperty("forage.dr.jdbc.datasource.name");
    }

    @Nested
    @DisplayName("Default Value Tests")
    class DefaultValueTests {

        @Test
        @DisplayName("Should return default enabled as true")
        void shouldReturnDefaultEnabled() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.enabled()).isTrue();
        }

        @Test
        @DisplayName("Should return default storage backend as jdbc")
        void shouldReturnDefaultStorageBackend() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.storageBackend()).isEqualTo("jdbc");
        }

        @Test
        @DisplayName("Should return default checkpoint enabled as true")
        void shouldReturnDefaultCheckpointEnabled() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.checkpointEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should return default shutdown persistence as true")
        void shouldReturnDefaultShutdownPersistence() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.shutdownPersistence()).isTrue();
        }

        @Test
        @DisplayName("Should return default replay on startup as true")
        void shouldReturnDefaultReplayOnStartup() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.replayOnStartup()).isTrue();
        }

        @Test
        @DisplayName("Should return default AI state check enabled as true")
        void shouldReturnDefaultAiStateCheckEnabled() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.aiStateCheckEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should return default snapshot TTL as 86400")
        void shouldReturnDefaultSnapshotTtlSeconds() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.snapshotTtlSeconds()).isEqualTo(86400L);
        }

        @Test
        @DisplayName("Should return default schema auto create as true")
        void shouldReturnDefaultSchemaAutoCreate() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.schemaAutoCreate()).isTrue();
        }

        @Test
        @DisplayName("Should return default JDBC datasource name as dataSource")
        void shouldReturnDefaultJdbcDatasourceName() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.jdbcDatasourceName()).isEqualTo("dataSource");
        }
    }

    @Nested
    @DisplayName("System Property Override Tests")
    class SystemPropertyOverrideTests {

        @Test
        @DisplayName("Should override enabled via system property")
        void shouldOverrideEnabled() {
            System.setProperty("forage.dr.enabled", "false");
            try {
                RecoveryConfig config = new RecoveryConfig();
                assertThat(config.enabled()).isFalse();
            } finally {
                System.clearProperty("forage.dr.enabled");
            }
        }

        @Test
        @DisplayName("Should override storage backend via system property")
        void shouldOverrideStorageBackend() {
            System.setProperty("forage.dr.storage.backend", "redis");
            try {
                RecoveryConfig config = new RecoveryConfig();
                assertThat(config.storageBackend()).isEqualTo("redis");
            } finally {
                System.clearProperty("forage.dr.storage.backend");
            }
        }

        @Test
        @DisplayName("Should override snapshot TTL via system property")
        void shouldOverrideSnapshotTtlSeconds() {
            System.setProperty("forage.dr.snapshot.ttl.seconds", "3600");
            try {
                RecoveryConfig config = new RecoveryConfig();
                assertThat(config.snapshotTtlSeconds()).isEqualTo(3600L);
            } finally {
                System.clearProperty("forage.dr.snapshot.ttl.seconds");
            }
        }
    }

    @Nested
    @DisplayName("Named Configuration Tests")
    class NamedConfigurationTests {

        @Test
        @DisplayName("Should use prefixed system property for enabled")
        void shouldUsePrefixedSystemPropertyForEnabled() {
            String prefix = "drtest1";
            System.setProperty("forage." + prefix + ".dr.enabled", "false");
            try {
                RecoveryConfig config = new RecoveryConfig(prefix);
                assertThat(config.enabled()).isFalse();
            } finally {
                System.clearProperty("forage." + prefix + ".dr.enabled");
            }
        }

        @Test
        @DisplayName("Should use prefixed system property for snapshot TTL")
        void shouldUsePrefixedSystemPropertyForSnapshotTtl() {
            String prefix = "drtest2";
            System.setProperty("forage." + prefix + ".dr.snapshot.ttl.seconds", "7200");
            try {
                RecoveryConfig config = new RecoveryConfig(prefix);
                assertThat(config.snapshotTtlSeconds()).isEqualTo(7200L);
            } finally {
                System.clearProperty("forage." + prefix + ".dr.snapshot.ttl.seconds");
            }
        }
    }

    @Nested
    @DisplayName("Config Interface Implementation Tests")
    class ConfigInterfaceTests {

        @Test
        @DisplayName("Should return correct module name")
        void shouldReturnCorrectName() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config.name()).isEqualTo("forage-dr-core");
        }

        @Test
        @DisplayName("Should create config without prefix")
        void shouldCreateConfigWithoutPrefix() {
            RecoveryConfig config = new RecoveryConfig();
            assertThat(config).isNotNull();
        }
    }
}
