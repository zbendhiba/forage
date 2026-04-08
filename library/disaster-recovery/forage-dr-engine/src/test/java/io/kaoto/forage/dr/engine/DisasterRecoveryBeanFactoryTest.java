package io.kaoto.forage.dr.engine;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DisasterRecoveryBeanFactory Tests")
class DisasterRecoveryBeanFactoryTest {

    private CamelContext camelContext;

    @AfterEach
    void tearDown() throws Exception {
        if (camelContext != null) {
            camelContext.close();
        }
    }

    @Nested
    @DisplayName("Disabled Configuration Tests")
    class DisabledConfigurationTests {

        @Test
        @DisplayName("Should not initialize when disabled")
        void shouldNotInitializeWhenDisabled() {
            System.setProperty("forage.dr.enabled", "false");
            try {
                camelContext = new DefaultCamelContext();
                DisasterRecoveryBeanFactory factory = new DisasterRecoveryBeanFactory();
                factory.setCamelContext(camelContext);

                factory.configure();

                assertThat(camelContext.getRoutePolicyFactories())
                        .noneMatch(f -> f instanceof ShutdownPersistenceRoutePolicyFactory);
            } finally {
                System.clearProperty("forage.dr.enabled");
            }
        }
    }

    @Nested
    @DisplayName("CamelContext Awareness Tests")
    class CamelContextAwarenessTests {

        @Test
        @DisplayName("Should set and get CamelContext")
        void shouldSetAndGetCamelContext() {
            camelContext = new DefaultCamelContext();
            DisasterRecoveryBeanFactory factory = new DisasterRecoveryBeanFactory();

            factory.setCamelContext(camelContext);

            assertThat(factory.getCamelContext()).isSameAs(camelContext);
        }
    }
}
