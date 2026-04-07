# Camel Observability Services - Analysis

Analysis of the [Camel Observability Services blog post](https://camel.apache.org/blog/2025/03/camel-observability/) (March 2025, by squakez) and its relevance to Forage.

## What is `camel-observability-services`?

Introduced in Camel 4.9.0, `camel-observability-services` is a single drop-in component that unifies Camel's previously scattered observability features. It is opinionated toward current industry standards and requires zero configuration by default.

## Four Pillars of Observability

| Pillar      | Technology              | Status in component       |
|-------------|-------------------------|---------------------------|
| **Metrics** | Micrometer Prometheus   | Included                  |
| **Health**  | Camel Health Checks     | Included                  |
| **Traces**  | OpenTelemetry           | Included                  |
| **Logs**    | (configured separately) | Not yet integrated        |

## Services Exposed Automatically

Adding the dependency is enough - no configuration required:

- **Health:** `/observe/health`, `/observe/health/live`, `/observe/health/ready` - Kubernetes liveness/readiness ready
- **Metrics:** `/observe/metrics` - Prometheus-ready endpoint, scrapable by Prometheus/Grafana
- **Traces:** OpenTelemetry built-in, requires a Java agent (except Quarkus which embeds a native client)
- **JMX:** full instrumentation for Jolokia, Hawtio, etc.

## Key Design Decisions

1. **Cross-runtime uniformity** - endpoints are identical regardless of runtime (Camel Main, Spring Boot, Quarkus)
2. **Zero-config by default** - just add `--dep camel:observability-services` and it works
3. **From Camel 4.11+**, the component will be included by default in `camel export`
4. **Traces** require an OpenTelemetry Java agent (`-javaagent:opentelemetry-javaagent.jar`), except on Quarkus which pushes traces directly to the collector

## Current State of Observability in Forage

Forage currently has no dedicated observability framework:

- **Logging:** SLF4J + Log4j2 throughout the codebase
- **JDBC pool metrics:** Agroal metrics enabled by default (active/idle connections, leak detection)
- **JMS pool monitoring:** via `JmsPoolConnectionFactory` (idle timeouts, check intervals)
- **Spring Boot Actuator:** JDBC/JMS starters are designed to work with Actuator health checks
- **Camel events:** `ForageContextServicePlugin` monitors Camel lifecycle events
- **Hot-reload:** `ForageReloadWatcher` with file system monitoring and detailed logging

No Micrometer, no OpenTelemetry, no standardized health endpoints, no structured logging.

## Relevance to Forage

The drop-in philosophy of `camel-observability-services` aligns directly with Forage's opinionated, zero-config approach to bean factories.

### Opportunities

- Forage could declare `camel-observability-services` as a dependency, giving all Forage-based applications full observability out of the box
- The existing Forage pool-level metrics (Agroal, JMS) would complement the Camel-level metrics exposed by the component
- Integration tests already cover Camel Main, Spring Boot, and Quarkus runtimes - validation of the integration would be straightforward
- Forage's configuration system (properties, env vars, system properties) maps naturally to the observability component's customization model
