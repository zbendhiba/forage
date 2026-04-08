# Disaster Recovery Brainstorm

Brainstorming ideas for a drop-in disaster recovery system for Camel routes running in pods. Inspired by the `camel-observability-services` approach: opinionated, zero-config by default, single dependency.

## Problem Statement

When a pod running Camel routes goes down and a new pod starts, all in-memory state is lost. This includes in-flight messages, partial aggregations, deduplication state, AI conversation context, and more. Users are often unaware of what state is at risk until they experience data loss in production.

## What State Is Lost When a Pod Dies?

### Camel-level state

| State | Risk | Notes |
|-------|------|-------|
| In-flight exchanges | Messages currently being processed are lost mid-route | Biggest source of data loss |
| Aggregation repositories | Partial aggregations stored in memory are lost | Common EIP, often left in-memory |
| Idempotent repositories | Deduplication state lost, causes reprocessing | Silent failures - duplicates appear |
| SAGA state | Long-running transaction compensation info lost | Cannot compensate interrupted sagas |
| Route policies | Flip/schedule policy state is ephemeral | Forage already has these |
| Resequencer buffers | Messages waiting to be reordered are lost | Less common but impactful |

### Forage-specific state

| State | Risk | Notes |
|-------|------|-------|
| Chat memory (in-memory) | AI conversation context lost | Forage has persistent providers (Infinispan, Redis) but in-memory is the default |
| Agent state | Multi-step agent progress lost | Agent must restart from scratch |
| Hot-reload watcher state | Minor, recoverable on restart | Low risk |

### Infrastructure state

| State | Risk | Notes |
|-------|------|-------|
| JDBC connection pools | Recreatable, but in-flight transactions are lost | Pool itself recovers, transactions don't |
| JMS sessions | Unacknowledged messages | Partially handled by JMS redelivery semantics |
| Kafka offsets | Uncommitted consumer offsets cause reprocessing | If auto-commit is disabled |

## How `camel-observability-services` Works (Reference Model)

Understanding how observability achieves transparency helps determine what's feasible for disaster recovery.

Observability uses Camel SPI extension points that wrap *around* routes without modifying route code:

- **`RoutePolicyFactory`** - applied to all routes automatically, intercepts route lifecycle
- **`InterceptStrategy`** - wraps all processors transparently (used by OpenTelemetry)
- **`HealthCheckRegistry`** - pluggable health check system
- **`ManagementStrategy`** - JMX instrumentation
- **`EventNotifier`** - notified of exchange lifecycle events

These are all **cross-cutting concerns** - they observe but don't change route behavior.

## Proposed Approach

A drop-in module (working name: `forage-disaster-recovery`) that automatically externalizes ephemeral state to persistent backends. Features are split into two categories based on how they integrate with Camel.

### Tier 1: Transparent (no route changes needed)

These features use Camel SPI extension points and work like `camel-observability-services` - just add the dependency and they activate automatically.

#### 1a. Graceful shutdown persistence

Uses **`RoutePolicyFactory`** to intercept shutdown signals.

On `SIGTERM` (Kubernetes pod termination signal):
- Drain in-flight exchanges to a persistent store
- On startup, detect and replay persisted exchanges

#### 1b. Checkpoint/resume for routes

Uses **`InterceptStrategy`** to wrap processors and snapshot exchange state at configurable points in a route. On restart, resume from the last checkpoint rather than reprocessing from the beginning.

#### 1c. Recovery event observability

Uses **`EventNotifier`** to detect exchange lifecycle events and emit metrics/traces for recovery operations (e.g., "X exchanges recovered on startup", "checkpoint latency").

#### 1d. AI state recovery (Forage-specific)

- Default chat memory and agent state to persistent backends
- Detect in-memory providers on startup in clustered/Kubernetes environments and log warnings
- Leverage existing Forage providers (Infinispan, Redis) as recovery backends

### Tier 2: Requires route-level changes or Camel SPI enhancements

These features cannot be applied transparently because the stateful components are declared *inside* route definitions (e.g., `aggregate().aggregationRepository(repo)`). They require either user-side route changes, or a deeper Camel SPI hook.

#### 2a. Automatic externalization of stateful EIPs

Swap in-memory repositories for persistent ones:

- Aggregation repository -> JDBC / Redis / Infinispan
- Idempotent repository -> JDBC / Redis / Infinispan
- This is the highest-value feature: users often forget to configure this

**Possible approaches:**
- **`ProcessorFactory`** - intercept creation of `AggregateProcessor` / `IdempotentConsumer` and replace in-memory repos with persistent ones. This is a deep hook and may require Camel core changes to be reliable.
- **Startup scanner** - on context start, scan all routes for in-memory repos, log warnings, and optionally swap them. Less invasive but may miss edge cases.
- **Convention-based** - provide a Forage-managed persistent repo that users reference by name in their routes (e.g., `aggregationRepository("#forage-aggregation-store")`). Simple but not zero-config.

#### 2b. SAGA journal externalization

Persist SAGA compensation actions so a new pod can complete or roll back interrupted long-running transactions. SAGA configuration is route-level, so the same constraints as 2a apply.

## Storage Backend

Following Forage's existing patterns (`ConfigEntries` + `AbstractConfig`), the recovery store would support multiple backends:

- **JDBC** - Forage already has `DataSourceProvider`
- **Redis** - Forage already has Redis chat memory provider
- **Infinispan** - Forage already has Infinispan chat memory provider

The existing Forage infrastructure for these backends can be reused directly.

## Decisions Summary

| Decision | Choice |
|----------|--------|
| Scope | Forage module |
| Focus | Tier 1 only (transparent, drop-in) |
| Granularity | All-or-nothing |
| Checkpoint frequency | Every exchange |
| Deployment | Storage-agnostic |
| Observability integration | Yes, but deferred |
| Cluster awareness | Deferred, but keep `camel-cluster-service` in mind |
| Idempotency | Camel's built-in `IdempotentConsumer` with shared persistent store |
| Schema management | Auto-create by default + migration scripts for production |
| Activation model | Zero-config (same as observability: add JAR, everything activates) |
| Relationship to observability | Independent modules (no dependency between DR and observability) |
| Snapshot identity | Auto-generated store ID (not exchange ID — exchange IDs are random per-pod) |
| Snapshot cleanup | TTL-based expiry, purge-on-load (same pattern as Camel IdempotentRepository) |
| Replay & re-checkpoint | No upsert needed — snapshots are append-only, expired ones are purged |

### Details

1. **Scope:** Forage module (not upstream Camel).
2. **Granularity:** All-or-nothing drop-in, applied to all routes. Per-route opt-out can be added later if needed.
3. **Focus:** Tier 1 features only (transparent, no route changes). Tier 2 is deferred.
4. **Checkpoint frequency:** Every exchange. Safety over throughput.
5. **Deployment model:** Storage-agnostic. Works anywhere (local dev, bare metal, Kubernetes). No k8s API dependency.
6. **Observability integration:** Yes, recovery events should emit metrics/traces. But deferred - not in the first iteration.
7. **Cluster awareness:** Deferred. Each pod manages its own recovery independently for now. Future work must consider `camel-cluster-service` interactions (leader election, checkpoint ownership, avoiding duplicate replays across pods).
8. **Idempotency:** Use Camel's built-in `IdempotentConsumer` EIP backed by the same persistent store as the checkpoint store. Replayed exchanges pass through the idempotent consumer - already-completed exchanges are skipped, incomplete ones are reprocessed. At-least-once delivery with dedup = effectively exactly-once from the user's perspective.
9. **Schema management:** Auto-create on startup by default (zero-config). Ship migration scripts alongside for production environments with restricted DB permissions. Users can disable auto-create via config.
10. **Activation model:** Zero-config, same philosophy as `camel-observability-services`. Add the JAR + a storage backend, DR activates with all features on. Users who add this dependency know what they're getting. `forage.dr.enabled=true` by default.
11. **Relationship to observability:** Fully independent. No dependency on `camel-observability-services`. DR logs its own recovery events via SLF4J. The `RecoveryEventNotifier` is a logger, not an OTel bridge. A future optional bridge could be added later if needed.
12. **Snapshot identity:** Exchange IDs are random UUIDs generated per-pod — they have no business meaning and are not unique across pods or restarts. Snapshots use an auto-generated store ID (BIGINT AUTO_INCREMENT) as PK. The exchange ID is kept as a regular column for logging/debugging only.
13. **Snapshot cleanup:** TTL-based expiry (default 24h, configurable via `forage.dr.snapshot.ttl.seconds`). Expired snapshots are purged on load — no explicit delete API, no background threads. Same pattern as Camel's `IdempotentRepository` with Redis/Infinispan TTL. This means `RecoveryStore` has only 4 methods: `saveCheckpoint`, `loadCheckpoints`, `saveShutdownExchange`, `loadShutdownExchanges`.
14. **Replay & re-checkpoint:** Since snapshots are append-only (no PK collision), a replayed exchange that gets re-checkpointed simply creates a new row. Old rows expire via TTL. No upsert logic needed.

## Implementation Plan

### Module Structure

```
library/disaster-recovery/
├── pom.xml                          # Aggregator
├── forage-dr-core/                  # Storage SPI, exchange serialization, config
├── forage-dr-engine/                # Camel SPI hooks (the drop-in entry point)
└── forage-dr-store-jdbc/            # JDBC backend (Redis/Infinispan deferred)
```

### Key Classes

#### `forage-dr-core` (no Camel dependency beyond API)

- **`RecoveryStore`** - storage interface (save/load checkpoints, save/load shutdown exchanges)
- **`RecoveryStoreProvider`** - extends `BeanProvider<RecoveryStore>`, discovered via ServiceLoader
- **`ExchangeSnapshot`** - POJO holding serialized exchange state (body, headers, properties, status)
- **`ExchangeSnapshotSerializer`** - converts between Camel `Exchange` and `ExchangeSnapshot`
- **`RecoveryConfigEntries` / `RecoveryConfig`** - standard Forage two-class config pattern

#### `forage-dr-engine` (the orchestrator)

- **`DisasterRecoveryBeanFactory`** - implements `BeanFactory`, registered via ServiceLoader. Single entry point discovered by `ForageContextServicePlugin`. Wires everything on context startup.
- **`ShutdownPersistenceRoutePolicyFactory` / `ShutdownPersistenceRoutePolicy`** - tracks in-flight exchanges, drains them to store on `SIGTERM`/`onStop()`
- **`CheckpointInterceptStrategy` / `CheckpointProcessor`** - wraps every processor, snapshots exchange state before delegation, marks completed after
- **`RecoveryReplayService`** - runs on `CamelContextStartedEvent`, replays persisted exchanges via `ProducerTemplate`, uses Camel's `IdempotentConsumer` for dedup
- **`AiStateRecoveryAdvisor`** - scans for in-memory AI providers, logs warnings
- **`RecoveryEventNotifier`** - stub for future observability hooks

#### `forage-dr-store-jdbc`

- **`JdbcRecoveryStore`** - reuses DataSource already in Camel registry from `DataSourceBeanFactory`
- **`JdbcRecoveryStoreProvider`** - `@ForageBean`, ServiceLoader-discovered
- **`SchemaInitializer`** - auto-creates tables (`forage_dr_checkpoint`, `forage_dr_shutdown_exchange`) + ships migration scripts in `db/migration/`

### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `forage.dr.enabled` | `true` | Master switch |
| `forage.dr.storage.backend` | `jdbc` | Storage backend |
| `forage.dr.checkpoint.enabled` | `true` | Enable checkpoint/resume |
| `forage.dr.shutdown.persistence` | `true` | Enable shutdown drain |
| `forage.dr.replay.on.startup` | `true` | Replay on restart |
| `forage.dr.ai.state.check.enabled` | `true` | Warn about in-memory AI providers |
| `forage.dr.snapshot.ttl.seconds` | `86400` | Snapshot TTL in seconds (default 24h). Expired snapshots purged on load. |
| `forage.dr.schema.auto.create` | `true` | Auto-create DB schema |
| `forage.dr.jdbc.datasource.name` | `dataSource` | Registry name of DataSource |

### Design Decisions

1. **BeanFactory ordering** - `DisasterRecoveryBeanFactory` needs the DataSource from `DataSourceBeanFactory`, but ServiceLoader gives no ordering. Solution: defer store initialization to `CamelContextStartedEvent` (same pattern as `ForageContextServicePlugin`).
2. **Separate core from engine** - storage backends depend on `forage-dr-core` only, not on the engine. Mirrors how `forage-core-jdbc` is independent from `forage-jdbc`.
3. **Exchange serialization** - non-serializable bodies/headers are skipped with warnings, snapshot marked as partial.
4. **Phase 1 replay is from route start** - checkpoints track progress per-processor, but initial replay re-injects from the route consumer. Mid-route resume is a future enhancement.

### Implementation Steps

```
Step 1: Create Maven module structure and POMs
  └─ Directory structure + pom.xml for dr-core, dr-engine, dr-store-jdbc
  └─ Wire into library/pom.xml
  └─ Status: [x] Done

Step 2: Implement forage-dr-core (blocked by Step 1)
  └─ 2.1 RecoveryStore interface (6 methods: save/load/delete for checkpoints and shutdown exchanges)
  └─ 2.2 RecoveryStoreProvider (extends BeanProvider<RecoveryStore>, ServiceLoader-discovered)
  └─ 2.3 SnapshotType enum (CHECKPOINT, SHUTDOWN) + ExchangeSnapshot POJO (8 fields)
  └─ 2.4 ExchangeSnapshotSerializer (Exchange <-> ExchangeSnapshot, handles non-serializable gracefully)
  └─ 2.5 RecoveryConfigEntries + RecoveryConfig (8 config properties, two-class pattern)
  └─ Status: [x] Done

Step 3: Implement forage-dr-store-jdbc (blocked by Step 2)
  └─ 3.1 JdbcRecoveryStore (RecoveryStore impl, plain JDBC, takes DataSource in constructor)
  └─ 3.2 SchemaInitializer (auto-creates tables) + V1__create_dr_tables.sql migration script
  └─ 3.3 JdbcRecoveryStoreProvider (@ForageBean, setDataSource() + create(), schema init inside create())
  └─ 3.3 META-INF/services registration for RecoveryStoreProvider
  └─ Status: [x] Done

Step 4: Implement ShutdownPersistenceRoutePolicyFactory (blocked by Step 2)
  └─ 4.1 ShutdownPersistenceRoutePolicy (extends RoutePolicySupport, tracks in-flight via ConcurrentHashMap)
  └─ 4.2 ShutdownPersistenceRoutePolicyFactory (creates one policy per route)
  └─ Design: onExchangeBegin tracks, onExchangeDone removes, onStop drains to store
  └─ No SIGTERM hooks needed — Camel lifecycle calls onStop() on shutdown
  └─ Status: [x] Done

Step 5: Implement CheckpointInterceptStrategy (blocked by Step 2)
  └─ 5.1 CheckpointProcessor (extends DelegateAsyncProcessor, snapshots before delegation)
  └─ 5.2 CheckpointInterceptStrategy (wraps every processor with CheckpointProcessor)
  └─ Design: DelegateAsyncProcessor preserves Camel's async engine
  └─ Checkpoint failure doesn't break processing (try/catch, log warning, continue)
  └─ Snapshots before delegation, not after (captures state before pod could die mid-processing)
  └─ Status: [x] Done

Step 6: Implement RecoveryReplayService + AiStateRecoveryAdvisor (blocked by Step 2)
  └─ Startup replay, AI provider warnings, event notifier stub
  └─ Status: [ ]

Step 7: Implement DisasterRecoveryBeanFactory (blocked by Steps 3-6)
  └─ The orchestrator that wires everything together
  └─ Status: [ ]
```

Steps 3, 4, 5, and 6 can be done in parallel once Step 2 is complete. Step 7 ties everything together at the end.

### Class Descriptions (forage-dr-core — Step 2)

All classes live in package `io.kaoto.forage.dr.core`.

#### RecoveryStore (interface)
Storage SPI — the contract any backend (JDBC, Redis, Infinispan) must implement. Only 4 methods:
- `saveCheckpoint` / `loadCheckpoints` — used by CheckpointInterceptStrategy (Step 5) and RecoveryReplayService (Step 6)
- `saveShutdownExchange` / `loadShutdownExchanges` — used by ShutdownPersistenceRoutePolicy (Step 4) and replay on startup
- No delete methods — snapshots expire via TTL, purged on load
- Lives in dr-core so backends depend only on this, not on the engine

#### RecoveryStoreProvider (interface)
Extends `BeanProvider<RecoveryStore>` — ServiceLoader discovery mechanism. The store is the *thing*, the provider is the *factory that creates it*. Standard Forage pattern (like ModelProvider, DataSourceProvider).

#### ExchangeSnapshot (POJO)
Serializable representation of a Camel Exchange at a point in time. Fields: id (auto-generated by store, BIGINT), exchangeId (kept for logging/debugging only — not PK), routeId, body (byte[]), headers (Map), properties (Map), snapshotType (CHECKPOINT|SHUTDOWN), timestamp, partial (boolean). Needed because Camel's Exchange is a runtime object tied to the context and can't be serialized directly.

#### ExchangeSnapshotSerializer
Converts between live Camel Exchange and ExchangeSnapshot. Handles non-serializable objects gracefully (skips with warnings, marks snapshot as partial). Kept separate from both POJO and store for clean separation.

#### RecoveryConfigEntries + RecoveryConfig
Standard Forage two-class config pattern. Controls 9 DR properties (enabled, storage backend, checkpoint on/off, shutdown persistence, replay on startup, AI state check, snapshot TTL in seconds (default 24h), schema auto-create, JDBC datasource name). Read by engine classes in Steps 4-7.

#### Dependency flow
```
RecoveryConfig ──> used by engine (Steps 4-7) to check settings
ExchangeSnapshot ──> stored/loaded by RecoveryStore
ExchangeSnapshotSerializer ──> used by engine to convert Exchange <-> ExchangeSnapshot
RecoveryStore ──> implemented by backends (Step 3: JDBC)
RecoveryStoreProvider ──> discovered by ServiceLoader, creates RecoveryStore instances
```

### Challenges to Watch

- **Async processor wrapping** - `CheckpointProcessor` must extend `DelegateAsyncProcessor` to not break Camel's async engine
- **Thread safety** - in-flight exchange tracking in shutdown policy needs proper synchronization
- **Graceful degradation** - if no storage backend is on classpath, disable DR with a clear log message
