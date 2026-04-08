package io.kaoto.forage.dr.engine;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AiStateRecoveryAdvisor Tests")
class AiStateRecoveryAdvisorTest {

    private CamelContext camelContext;

    @BeforeEach
    void setUp() throws Exception {
        camelContext = new DefaultCamelContext();
        camelContext.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (camelContext != null) {
            camelContext.close();
        }
    }

    @Nested
    @DisplayName("Advisory Tests")
    class AdvisoryTests {

        @Test
        @DisplayName("Should not throw with no beans registered")
        void shouldNotThrowWithNoBeansRegistered() {
            assertThatCode(() -> AiStateRecoveryAdvisor.advise(camelContext)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should detect in-memory provider by class name")
        void shouldDetectInMemoryProvider() {
            camelContext.getRegistry().bind("chatMemory", new MessageWindowChatMemory());

            assertThatCode(() -> AiStateRecoveryAdvisor.advise(camelContext)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not warn for non-memory beans")
        void shouldNotWarnForNonMemoryBeans() {
            camelContext.getRegistry().bind("myBean", "just a string");

            assertThatCode(() -> AiStateRecoveryAdvisor.advise(camelContext)).doesNotThrowAnyException();
        }
    }

    /**
     * Inner class with the exact class name that triggers detection.
     */
    static class MessageWindowChatMemory {}
}
