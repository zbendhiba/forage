package io.kaoto.forage.dr.engine;

import java.util.Collections;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.RoutePolicy;
import io.kaoto.forage.dr.core.ExchangeSnapshot;
import io.kaoto.forage.dr.core.RecoveryStore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShutdownPersistenceRoutePolicyFactory Tests")
class ShutdownPersistenceRoutePolicyFactoryTest {

    private CamelContext camelContext;
    private ShutdownPersistenceRoutePolicyFactory factory;

    @BeforeEach
    void setUp() {
        camelContext = new DefaultCamelContext();
        factory = new ShutdownPersistenceRoutePolicyFactory(new StubRecoveryStore());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (camelContext != null) {
            camelContext.close();
        }
    }

    @Nested
    @DisplayName("Policy Creation Tests")
    class PolicyCreationTests {

        @Test
        @DisplayName("Should create ShutdownPersistenceRoutePolicy")
        void shouldCreateShutdownPersistenceRoutePolicy() {
            RoutePolicy policy = factory.createRoutePolicy(camelContext, "route1", null);

            assertThat(policy).isNotNull().isInstanceOf(ShutdownPersistenceRoutePolicy.class);
        }

        @Test
        @DisplayName("Should create different policy instance per route")
        void shouldCreateDifferentPolicyPerRoute() {
            RoutePolicy p1 = factory.createRoutePolicy(camelContext, "route1", null);
            RoutePolicy p2 = factory.createRoutePolicy(camelContext, "route2", null);

            assertThat(p1).isNotSameAs(p2);
        }
    }

    static class StubRecoveryStore implements RecoveryStore {
        @Override
        public void saveCheckpoint(ExchangeSnapshot snapshot) {}

        @Override
        public List<ExchangeSnapshot> loadCheckpoints(String routeId) {
            return Collections.emptyList();
        }

        @Override
        public void saveShutdownExchange(ExchangeSnapshot snapshot) {}

        @Override
        public List<ExchangeSnapshot> loadShutdownExchanges() {
            return Collections.emptyList();
        }
    }
}
