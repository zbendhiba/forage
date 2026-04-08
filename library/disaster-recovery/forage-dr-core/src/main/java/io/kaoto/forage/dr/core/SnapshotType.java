package io.kaoto.forage.dr.core;

/**
 * Indicates why an exchange snapshot was taken.
 */
public enum SnapshotType {

    /** Snapshot taken at a processing checkpoint during normal execution. */
    CHECKPOINT,

    /** Snapshot taken during graceful shutdown to preserve in-flight exchanges. */
    SHUTDOWN
}
