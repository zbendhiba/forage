-- Forage Disaster Recovery schema
-- Tables for persisting exchange snapshots (checkpoints and shutdown drains)
-- Snapshots expire based on TTL — purged on load, not explicitly deleted

CREATE TABLE IF NOT EXISTS forage_dr_checkpoint (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    exchange_id VARCHAR(255),
    route_id    VARCHAR(255) NOT NULL,
    body        BLOB,
    headers     BLOB,
    properties  BLOB,
    snapshot_type VARCHAR(20) NOT NULL,
    created_at  BIGINT NOT NULL,
    partial     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_dr_checkpoint_route ON forage_dr_checkpoint (route_id);
CREATE INDEX IF NOT EXISTS idx_dr_checkpoint_created ON forage_dr_checkpoint (created_at);

CREATE TABLE IF NOT EXISTS forage_dr_shutdown_exchange (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    exchange_id VARCHAR(255),
    route_id    VARCHAR(255) NOT NULL,
    body        BLOB,
    headers     BLOB,
    properties  BLOB,
    snapshot_type VARCHAR(20) NOT NULL,
    created_at  BIGINT NOT NULL,
    partial     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_dr_shutdown_created ON forage_dr_shutdown_exchange (created_at);
