package io.kaoto.forage.dr.core;

import java.util.Map;
import java.util.Optional;
import io.kaoto.forage.core.util.config.ConfigEntries;
import io.kaoto.forage.core.util.config.ConfigEntry;
import io.kaoto.forage.core.util.config.ConfigModule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RecoveryConfigEntries Tests")
class RecoveryConfigEntriesTest {

    @Nested
    @DisplayName("Static ConfigModule Field Tests")
    class StaticConfigModuleTests {

        @Test
        @DisplayName("Should have correct ConfigModule names")
        void shouldHaveCorrectConfigModuleNames() {
            assertThat(RecoveryConfigEntries.ENABLED.name()).isEqualTo("forage.dr.enabled");
            assertThat(RecoveryConfigEntries.STORAGE_BACKEND.name()).isEqualTo("forage.dr.storage.backend");
            assertThat(RecoveryConfigEntries.CHECKPOINT_ENABLED.name()).isEqualTo("forage.dr.checkpoint.enabled");
            assertThat(RecoveryConfigEntries.SHUTDOWN_PERSISTENCE.name()).isEqualTo("forage.dr.shutdown.persistence");
            assertThat(RecoveryConfigEntries.REPLAY_ON_STARTUP.name()).isEqualTo("forage.dr.replay.on.startup");
            assertThat(RecoveryConfigEntries.AI_STATE_CHECK_ENABLED.name())
                    .isEqualTo("forage.dr.ai.state.check.enabled");
            assertThat(RecoveryConfigEntries.SNAPSHOT_TTL_SECONDS.name()).isEqualTo("forage.dr.snapshot.ttl.seconds");
            assertThat(RecoveryConfigEntries.SCHEMA_AUTO_CREATE.name()).isEqualTo("forage.dr.schema.auto.create");
            assertThat(RecoveryConfigEntries.JDBC_DATASOURCE_NAME.name()).isEqualTo("forage.dr.jdbc.datasource.name");
        }

        @Test
        @DisplayName("Should have correct ConfigModule config class references")
        void shouldHaveCorrectConfigModuleConfigClassReferences() {
            assertThat(RecoveryConfigEntries.ENABLED.config()).isEqualTo(RecoveryConfig.class);
            assertThat(RecoveryConfigEntries.STORAGE_BACKEND.config()).isEqualTo(RecoveryConfig.class);
            assertThat(RecoveryConfigEntries.CHECKPOINT_ENABLED.config()).isEqualTo(RecoveryConfig.class);
            assertThat(RecoveryConfigEntries.SHUTDOWN_PERSISTENCE.config()).isEqualTo(RecoveryConfig.class);
            assertThat(RecoveryConfigEntries.REPLAY_ON_STARTUP.config()).isEqualTo(RecoveryConfig.class);
            assertThat(RecoveryConfigEntries.AI_STATE_CHECK_ENABLED.config()).isEqualTo(RecoveryConfig.class);
            assertThat(RecoveryConfigEntries.SNAPSHOT_TTL_SECONDS.config()).isEqualTo(RecoveryConfig.class);
            assertThat(RecoveryConfigEntries.SCHEMA_AUTO_CREATE.config()).isEqualTo(RecoveryConfig.class);
            assertThat(RecoveryConfigEntries.JDBC_DATASOURCE_NAME.config()).isEqualTo(RecoveryConfig.class);
        }
    }

    @Nested
    @DisplayName("CONFIG_MODULES Map Tests")
    class ConfigModulesMapTests {

        @Test
        @DisplayName("Should return immutable map from entries()")
        void shouldReturnImmutableMapFromEntries() {
            Map<ConfigModule, ConfigEntry> entries = ConfigEntries.entriesOf(RecoveryConfigEntries.class);

            assertThat(entries).isNotNull();
            assertThat(entries).isNotEmpty();

            assertThatThrownBy(entries::clear).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should contain all ConfigModules in entries map")
        void shouldContainAllConfigModulesInEntriesMap() {
            Map<ConfigModule, ConfigEntry> entries = ConfigEntries.entriesOf(RecoveryConfigEntries.class);

            assertThat(entries).containsKey(RecoveryConfigEntries.ENABLED);
            assertThat(entries).containsKey(RecoveryConfigEntries.STORAGE_BACKEND);
            assertThat(entries).containsKey(RecoveryConfigEntries.CHECKPOINT_ENABLED);
            assertThat(entries).containsKey(RecoveryConfigEntries.SHUTDOWN_PERSISTENCE);
            assertThat(entries).containsKey(RecoveryConfigEntries.REPLAY_ON_STARTUP);
            assertThat(entries).containsKey(RecoveryConfigEntries.AI_STATE_CHECK_ENABLED);
            assertThat(entries).containsKey(RecoveryConfigEntries.SNAPSHOT_TTL_SECONDS);
            assertThat(entries).containsKey(RecoveryConfigEntries.SCHEMA_AUTO_CREATE);
            assertThat(entries).containsKey(RecoveryConfigEntries.JDBC_DATASOURCE_NAME);
        }
    }

    @Nested
    @DisplayName("find() Method Tests")
    class FindMethodTests {

        @Test
        @DisplayName("Should find ConfigModule without prefix")
        void shouldFindConfigModuleWithoutPrefix() {
            Optional<ConfigModule> found = ConfigEntries.find(
                    ConfigEntries.getModules(RecoveryConfigEntries.class), null, "forage.dr.enabled");

            assertThat(found).isPresent();
            assertThat(found.get()).isEqualTo(RecoveryConfigEntries.ENABLED);
        }

        @Test
        @DisplayName("Should return Optional.empty for unknown configuration name")
        void shouldReturnEmptyForUnknownConfigurationName() {
            assertThat(ConfigEntries.find(
                            ConfigEntries.getModules(RecoveryConfigEntries.class), null, "unknown.config"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("register() Method Tests")
    class RegisterMethodTests {

        @Test
        @DisplayName("Should register configurations without prefix")
        void shouldRegisterConfigurationsWithoutPrefix() {
            ConfigEntries.registerPrefix(RecoveryConfigEntries.class, null);

            RecoveryConfig config = new RecoveryConfig();
            assertThat(config).isNotNull();
        }

        @Test
        @DisplayName("Should register configurations with prefix")
        void shouldRegisterConfigurationsWithPrefix() {
            String prefix = "testdr";

            ConfigEntries.registerPrefix(RecoveryConfigEntries.class, prefix);

            System.setProperty("forage." + prefix + ".dr.enabled", "false");

            try {
                RecoveryConfig config = new RecoveryConfig(prefix);
                assertThat(config.enabled()).isFalse();
            } finally {
                System.clearProperty("forage." + prefix + ".dr.enabled");
            }
        }
    }
}
