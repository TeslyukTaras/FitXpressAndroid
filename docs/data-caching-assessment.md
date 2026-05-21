# Persistent Data Caching — Feasibility Assessment

> Goal: stop refetching wearable data on every screen visit. Cache at least the
> last ~30 days locally and evict anything older. **Assessment only — not yet implemented.**

## How data loads today

Every wearable screen goes through one of three repositories —
`SleepRepository`, `ActivityRepository`, `RecoveryRepository` — and they all
share an identical shape:

```kotlin
getSessionsForRange(start, end): Result<List<T>>   // + a getXForDate that delegates to it
```

Caching that exists right now: `TtlCache` — **in-memory, process-wide** (repos are
Koin singletons), keyed by the exact `(start, end)` pair, with a **60-second TTL**
(`TerraCacheConstants.RANGE_CACHE_TTL_MS`).

So "loads every time" comes from three things:

1. **60s TTL** — older than a minute → refetch.
2. **In-memory only** — a cold start (process death) loses everything; the first
   visit to each screen refetches.
3. **Exact-range keying** — Sleep-day, Sleep-summary, and Recovery-day request
   different windows, so they don't share cache even when the days overlap.

`RecoveryRepository` is **derived** — it composes Sleep + Activity and has no Terra
endpoint of its own. If Sleep and Activity get persistent caching, **Recovery
benefits for free.**

## Is it hard to integrate? No — the architecture is well-suited

- **One choke point per domain** (`getSessionsForRange`). All ViewModels already go
  through these and already handle `Result`, so caching changes never reach the UI.
- Repos are **interfaces with single Koin bindings** — you can wrap them with a
  caching decorator or swap the implementation in `AppModule` without touching
  anything else.
- The models (`SleepSession`, `ActivitySummary`, `RecoverySnapshot`) are **plain
  data classes** — trivial to persist, and `kotlinx-serialization` is already in
  the project.
- There's already a cache abstraction (`TtlCache`) and a constants convention to
  slot into.

**No ViewModel or Compose changes are required.** The work is localized to `data/*`.

## Options, easiest → most robust

1. **Bump the TTL** (1 line). Helps within a session, useless across cold starts.
   Band-aid.
2. **Persistent, day-keyed store — recommended.** Add Room (or SQLDelight): one
   table per domain, or a generic `(domain, date, payloadJson)` table. Repo logic
   becomes: *read requested days from DB → fetch only the missing/stale days from
   Terra → upsert → return.* Evict rows older than 30 days on a periodic prune.
   Matches the request exactly. **Effort: ~1–2 days.** Room needs the KSP plugin
   added (not present yet — one line).
3. **JSON blob in DataStore** (no new processor; uses existing serialization).
   Avoids adding Room/KSP, but you hand-roll range queries and eviction, which gets
   awkward. Fine for a quick version, worse long-term.

## The decisions that actually matter (not the plumbing)

- **Switch the cache key from range to individual day.** Highest-impact change:
  overlapping windows then share cache, and "delete older than a month" becomes a
  trivial delete-by-date.
- **Freshness policy for recent vs old days.** Past days are effectively immutable →
  cache indefinitely (until the 30-day prune). **Today (and maybe yesterday) keep
  changing** → short TTL or always-refetch. This "old = permanent, recent = short
  TTL" split is the only real logic nuance.
- **Negative caching.** Distinguish "no data for this day" from "not fetched yet," so
  you don't refetch days that genuinely have nothing.
- **Scope:** Terra-only. Scan/body data (3DLook + Firestore) is a separate path and
  out of scope.

## Verdict

Low-to-moderate effort, low risk. The repository boundary is clean and consistent,
so it's a contained data-layer change. The cleanest target design is:

> **Room + day-keyed caching + a recent-day TTL + a 30-day prune** — and Recovery
> comes along for free.

## Relevant files

- `data/terra/TtlCache.kt` — current in-memory cache
- `data/sleep/TerraApiSleepRepository.kt`, `data/sleep/SleepRepository.kt`
- `data/activity/TerraApiActivityRepository.kt`, `data/activity/ActivityRepository.kt`
- `data/recovery/TerraDerivedRecoveryRepository.kt` (derived; no own endpoint)
- `utils/constants/TerraConstants.kt` → `TerraCacheConstants.RANGE_CACHE_TTL_MS`
- `di/AppModule.kt` — repository bindings (lines ~119–122)
