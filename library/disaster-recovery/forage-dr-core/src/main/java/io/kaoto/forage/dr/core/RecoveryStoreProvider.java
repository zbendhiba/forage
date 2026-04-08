package io.kaoto.forage.dr.core;

import io.kaoto.forage.core.common.BeanProvider;

/**
 * Provider interface for creating {@link RecoveryStore} instances.
 * Discovered via ServiceLoader. Backend modules (JDBC, Redis, etc.)
 * implement this to supply their store implementation.
 */
public interface RecoveryStoreProvider extends BeanProvider<RecoveryStore> {}
