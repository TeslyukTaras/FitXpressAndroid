package com.hexis.bi.data.terra

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide in-memory TTL cache. Last-write-wins; concurrent callers may both fetch — fine for
 * the short-lived usage where the second call lands a fraction of a second after the first and
 * just overwrites the same value.
 *
 * Entries also carry the [generation] they were fetched at. When [generation] advances (e.g. a Terra
 * sync lands new data), older entries miss on the next [get] and refetch lazily — no explicit
 * invalidation call, and a sync that arrives while no one is reading is still picked up later.
 */
class TtlCache<K : Any, V : Any>(
    private val ttlMs: Long,
    private val generation: () -> Long = { 0L },
) {

    private data class Entry<V : Any>(val timestampMs: Long, val generation: Long, val value: V)

    private val mutex = Mutex()
    private val map = HashMap<K, Entry<V>>()

    suspend fun get(key: K): V? = mutex.withLock {
        val entry = map[key] ?: return@withLock null
        val expired = System.currentTimeMillis() - entry.timestampMs >= ttlMs
        if (expired || entry.generation != generation()) {
            map.remove(key)
            null
        } else entry.value
    }

    suspend fun put(key: K, value: V) = mutex.withLock {
        map[key] = Entry(System.currentTimeMillis(), generation(), value)
    }
}
