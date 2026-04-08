package io.kaoto.forage.dr.engine;

import java.util.Set;
import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans the Camel registry on startup for in-memory AI providers and logs
 * warnings suggesting persistent alternatives.
 *
 * <p>In-memory chat memory and agent state are lost on pod restart. This advisor
 * detects common in-memory provider class names in the registry and warns
 * the user to switch to persistent backends (Redis, Infinispan) for production
 * environments where disaster recovery matters.</p>
 */
public final class AiStateRecoveryAdvisor {

    private static final Logger LOG = LoggerFactory.getLogger(AiStateRecoveryAdvisor.class);

    private static final Set<String> IN_MEMORY_CLASS_NAMES =
            Set.of("MessageWindowChatMemoryBeanProvider", "MessageWindowChatMemory");

    private AiStateRecoveryAdvisor() {}

    /**
     * Scans the Camel registry for in-memory AI providers and logs warnings.
     *
     * @param camelContext the started CamelContext
     */
    public static void advise(CamelContext camelContext) {
        Set<String> beanNames =
                camelContext.getRegistry().findByTypeWithName(Object.class).keySet();

        for (String beanName : beanNames) {
            Object bean = camelContext.getRegistry().lookupByName(beanName);
            if (bean == null) {
                continue;
            }

            String className = bean.getClass().getSimpleName();
            if (IN_MEMORY_CLASS_NAMES.contains(className)) {
                LOG.warn(
                        "Detected in-memory AI provider '{}' ({}). "
                                + "In-memory chat memory is lost on pod restart. "
                                + "Consider using a persistent backend (Redis, Infinispan) "
                                + "for production environments with disaster recovery enabled.",
                        beanName,
                        className);
            }
        }
    }
}
