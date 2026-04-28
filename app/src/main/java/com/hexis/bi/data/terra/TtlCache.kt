package com.hexis.bi.data.terra

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide in-memory TTL cache. Last-write-wins; concurrent callers may both fetch — fine for
 * the short-lived usage where the second call lands a fraction of a second after the first and
 * just overwrites the same value.
 */
class TtlCache<K : Any, V : Any>(private val ttlMs: Long) {

    private data class Entry<V : Any>(val timestampMs: Long, val value: V)

    private val mutex = Mutex()
    private val map = HashMap<K, Entry<V>>()

    suspend fun get(key: K): V? = mutex.withLock {
        val entry = map[key] ?: return@withLock null
        if (System.currentTimeMillis() - entry.timestampMs >= ttlMs) {
            map.remove(key)
            null
        } else entry.value
    }

    suspend fun put(key: K, value: V) = mutex.withLock {
        map[key] = Entry(System.currentTimeMillis(), value)
    }

    suspend fun clear() = mutex.withLock { map.clear() }
}
