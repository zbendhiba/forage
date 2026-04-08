# Disaster Recovery — Manual Test

Manual validation of the DR module with Camel JBang before writing automated integration tests.

## Step 1: Build Forage

Build and install all Forage modules locally so the DR JARs are available to JBang:

```bash
cd /path/to/forage
mvn clean install -DskipTests
```

## Step 2: Install the Forage JBang Plugin

If you already have an older version installed, remove it first:

```bash
camel plugin delete forage
```

Then install the locally-built version:

```bash
camel plugin add forage \
  --command='forage' \
  --description='Forage Camel JBang Plugin' \
  --artifactId='camel-jbang-plugin-forage' \
  --groupId='io.kaoto.forage' \
  --version='1.2-SNAPSHOT' \
  --gav='io.kaoto.forage:camel-jbang-plugin-forage:1.2-SNAPSHOT'
```

## Step 3: Start PostgreSQL

In a separate terminal:

```bash
camel infra run postgres
```

This starts a PostgreSQL container on `localhost:5432` with user `test` / password `test`.

Wait until you see it's ready before continuing.

## Step 4: Run the DR Route

```bash
cd library/disaster-recovery/manual-test
camel run --dep=mvn:io.kaoto.forage:forage-dr-engine:1.2-SNAPSHOT,mvn:io.kaoto.forage:forage-dr-store-jdbc:1.2-SNAPSHOT \
  dr-route.camel.yaml forage-dr.properties forage-datasource-factory.properties
```

> **Note:** The DR module is not yet catalog-driven, so `--dep` is needed to put the DR JARs on the classpath. The Forage plugin (`camel forage run`) is not required — plain `camel run` works.

### What to look for on startup

1. `Disaster recovery schema initialized` — DR tables created in PostgreSQL
2. `Disaster recovery enabled — deferring initialization to context start`
3. `Shutdown persistence enabled — in-flight exchanges will be drained on shutdown`
4. `Checkpoint/resume enabled — exchange state will be checkpointed at each processor`
5. `Disaster recovery fully initialized (backend: jdbc, TTL: 86400s)`

### What to look for during processing

The timer fires every 3 seconds. You should see:

```
[Step 1] Received: Message #1 at 14:30:05
[Step 2] Processing: Message #1 at 14:30:05
[Processor] Output: Processed: Message #1 at 14:30:05
[Step 3] Done: Processed: Message #1 at 14:30:05
```

Checkpoint saves should happen at each processor step (visible in TRACE/DEBUG logging or in the database).

## Step 5: Verify Database State

In another terminal, connect to PostgreSQL:

```bash
psql -h localhost -U test -d postgres
```

Inspect the DR tables:

```sql
-- Check if tables were created
\dt forage_dr_*

-- Check checkpoint snapshots
SELECT id, exchange_id, route_id, snapshot_type, created_at, partial
FROM forage_dr_checkpoint
ORDER BY created_at DESC
LIMIT 10;

-- Check shutdown exchange table (empty until shutdown)
SELECT id, exchange_id, route_id, snapshot_type, created_at, partial
FROM forage_dr_shutdown_exchange
ORDER BY created_at DESC
LIMIT 10;
```

## Step 6: Test Shutdown Persistence

1. Press **Ctrl+C** in the terminal running the route
2. Look for shutdown drain log messages (in-flight exchanges persisted)
3. Check the `forage_dr_shutdown_exchange` table — it should now have rows:

```sql
SELECT * FROM forage_dr_shutdown_exchange ORDER BY created_at DESC LIMIT 5;
```

## Step 7: Test Recovery Replay

1. Restart the route:

   ```bash
   camel forage run dr-route.camel.yaml forage-dr.properties forage-datasource-factory.properties
   ```

2. Look for replay log messages on startup — saved shutdown exchanges should be replayed into their original routes

## Step 8: Test DR Disabled

1. Edit `forage-dr.properties` and set `forage.dr.enabled=false`
2. Restart the route
3. Confirm you see: `Disaster recovery is disabled (forage.dr.enabled=false)`
4. Verify no new rows appear in the DR tables
5. Restore `forage.dr.enabled=true` when done

## Cleanup

Stop PostgreSQL:

```bash
# In the terminal running camel infra
Ctrl+C
```

Or remove the container directly:

```bash
docker rm -f $(docker ps -q --filter ancestor=postgres)
```

## Configuration Reference

### forage-dr.properties

| Property | Value | Description |
|----------|-------|-------------|
| `forage.dr.enabled` | `true` | Master switch |
| `forage.dr.checkpoint.enabled` | `true` | Checkpoint at each processor |
| `forage.dr.shutdown.persistence` | `true` | Drain on shutdown |
| `forage.dr.replay.on.startup` | `true` | Replay on restart |
| `forage.dr.snapshot.ttl.seconds` | `86400` | 24h TTL |
| `forage.dr.schema.auto.create` | `true` | Auto-create DB tables |

### forage-datasource-factory.properties

PostgreSQL datasource used by the DR JDBC store. Uses the default bean name `dataSource`.

## Resume Checklist

If picking this up after a break:

1. **Run unit tests** to verify nothing is broken:

   ```bash
   mvn verify -f library/disaster-recovery
   ```

2. **Start PostgreSQL and run the route:**

   ```bash
   camel infra run postgres
   # In another terminal:
   cd library/disaster-recovery/manual-test
   camel run --dep=mvn:io.kaoto.forage:forage-dr-engine:1.2-SNAPSHOT,mvn:io.kaoto.forage:forage-dr-store-jdbc:1.2-SNAPSHOT \
     dr-route.camel.yaml forage-dr.properties forage-datasource-factory.properties
   ```

3. **Continue manual validation** (Steps 5-8 above): check DB tables, test shutdown drain, test restart replay, test DR disabled.

4. **Commit** once validated.

## Open Items

Documented in `docs/disaster-recovery-brainstorm.md` under "Future Work":

- **Catalog integration** — DR module is not yet catalog-driven; `--dep` is required to load the JARs. Needs changes to the catalog plugin to support auto-activating BeanFactories.
- **Automated integration tests** — Citrus framework tests planned after manual validation confirms real-world behavior.
- **Runtime adapters** — Quarkus and Spring Boot runtime adapters not yet implemented.
