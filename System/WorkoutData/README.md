# WorkoutData

A frame-agnostic ContentProvider that stores per-second workout history and the planned
(future) workout profile, and shares both across processes/APKs. It replaces the
per-metric `DATA_*_GRAPH_DATA_PARCELABLE` DataMatrix keys for graphing: writers push raw
data points once, and any reader queries whatever slice it needs.

## Architecture

```
 Writer APK (holds WRITE permission)                Reader APKs (no permission needed)
 ┌─────────────────────────┐                        ┌──────────────────────────┐
 │  WorkoutTrackingService  │                       │  Graph UIs, summaries, …  │
 └──────────┬──────────────┘                        └──────────┬───────────────┘
            │ WorkoutDataClient                                │ WorkoutDataClient
            ▼                                                  ▼
 ┌──────────────────────────────────────────────────────────────────────────────┐
 │ WorkoutContentProvider   authority: com.jht.workoutdata.provider             │
 │   writes: call()  — addPoint / setFuture / clear   (permission-gated)        │
 │   reads:  query() — Cursor over CursorWindow (no Binder size limit)          │
 │           call()  — getFuture / getGeneration (small payloads, open)         │
 │   notifies: /points on history change · /future on future change            │
 └──────────────────────────────┬───────────────────────────────────────────────┘
                                │ WorkoutDataRepository (interface)
                                ▼
                  RoomWorkoutDataRepository — in-memory Room DB
                  (swap to a file-backed builder in WorkoutDatabase to persist)
```

Key files: `WorkoutDataContract` (URIs, methods, columns, permission),
`WorkoutContentProvider`, `WorkoutDataRepository` / `RoomWorkoutDataRepository`,
`WorkoutDataDao`, `WorkoutDataClient` (use this, not raw resolver calls), and the models
`WorkoutDataPoint`, `MetricSample`, `WorkoutFutureData`.

## Data model & invariants

- **Time axis is `elapsedTimeSec`** — whole seconds of workout time, expected ~1 point/sec
  while the workout runs. There is no wall-clock timestamp.
- **One row per second.** `elapsed_time_sec` is unique; adding a point for an existing
  second **overrides** it (last write wins). Poll-timer jitter can't inflate the data.
- **Frame-agnostic.** Writers fill only the fields their machine has (speed/incline for a
  treadmill, resistance/rpm for a bike, …) and leave the rest at defaults. Readers pick
  metrics via `WorkoutMetric`.
- **Values are stored raw.** `heartRate <= 0` means "no reading" (e.g. no strap); the graph
  layers map it to the shared `INVALID_GRAPH_DATA` sentinel and render a gap rather than a
  line at zero. Consumers doing their own math should filter non-positive heart rates.
- **`clear()` starts a new workout**: wipes history *and* the future profile. Elapsed time
  restarts at 0.
- **Generation token** (`getGeneration()` / `METHOD_GET_GENERATION`): changes on every
  `clear()` and on provider-process restart, otherwise stable. Incremental readers compare
  it between reads to detect that their cached history belongs to an earlier workout —
  elapsed-second continuity alone cannot detect a clear once the new workout's clock passes
  the old one.
- **Retention cap**: at most `28,800` rows (~8 h at 1 Hz); oldest rows are trimmed.
- **The future profile is in-memory only** — it is transient script/terrain data,
  replaced wholesale by `setFuture` and lost if the provider process dies (publishers
  re-push it; readers re-fetch on the change notification).

## Permissions

Reads are open to any app. Writes require the signature-level permission (granted only to
APKs signed with the platform key):

```xml
<uses-permission android:name="com.jht.workoutdata.permission.WRITE" />
```

Enforced in code inside `call()` — the framework does not apply `android:writePermission`
to `call()`. Callers without it get a `SecurityException`.

## Usage

Always go through `WorkoutDataClient`. Suspend functions hop to `Dispatchers.IO`
themselves; the `*Blocking` variants run on the **calling** thread and must be kept off
the main thread (they do cross-process I/O).

### Writing (workout engine side)

```kotlin
val client = WorkoutDataClient(context)

// once per second while the workout runs (writes are synchronous & cheap)
client.addPoint(
    WorkoutDataPoint(
        elapsedTimeSec = elapsedSec,   // the logical key; same second = override
        speedKph = speed,
        inclinePercent = incline,
        heartRate = hr,
    )
)

// when a new workout starts
client.clearBlocking()

// whenever the scripted/terrain profile changes
client.setFuture(
    WorkoutFutureData(listOf(
        FutureProfile(WorkoutMetric.INCLINE, listOf(
            FutureProfilePoint(timeSec = 30f, value = 4.5f),
            FutureProfilePoint(timeSec = 90f, value = 6.0f),
        )),
    ))
)
```

From Java, build points with the fluent builder:

```java
WorkoutDataPoint point = new WorkoutDataPoint.Builder(elapsedSec)
        .speedKph(speed)
        .heartRate(hr)
        .build();
client.addPoint(point);
```

### Reading history

The two query knobs compose freely — a time window, a down-sample interval, or both:

```kotlin
client.queryPoints()                                   // everything
client.queryPoints(WorkoutDataQuery.lastMinutes(30))   // last 30 minutes
client.queryPoints(WorkoutDataQuery.everySeconds(5))   // 1 point per 5s bucket
client.queryPoints(WorkoutDataQuery.lastMinutes(30, 5))// last 30 min @ 5s resolution
client.queryPointsBetweenBlocking(120, 180)            // absolute range 120..180s
client.queryPointsSinceBlocking(lastSeenSec + 1)       // incremental: only what's new
```

The `since` read is the incremental-consumer idiom: keep the newest second you've
processed, and on each history-change notification fetch only what you're missing —
no sentinel `Int.MAX_VALUE`, and dropped notifications self-heal on the next read.

Windows are relative to the **latest stored second**, not the clock. Down-sampling keeps
the latest point of each bucket. For one metric as a lightweight series:

```kotlin
val hr: List<MetricSample> =
    client.querySeries(WorkoutMetric.HEART_RATE, WorkoutDataQuery.lastMinutes(30))
```

For very large custom reads, `client.openPointsCursor(query)` hands you the raw Cursor
(columns in `WorkoutDataContract`); you own closing it.

### Reading the future profile

```kotlin
val future = client.getFuture()                        // suspend; or getFutureBlocking()
val incline = future.profileFor(WorkoutMetric.INCLINE) // null if not scripted
```

### Change notifications (replaces polling / DataMatrix events)

Two independent triggers, so you know *what* changed:

```kotlin
// fires ~1/sec while a workout runs, and on clear
val historyObserver = client.registerChangeListener(handler) { redrawGraph() }

// fires only when the future profile is replaced or cleared (rare)
val futureObserver = client.registerFutureChangeListener(handler) { refetchFuture() }

// when done listening
client.unregisterChangeListener(historyObserver)
client.unregisterChangeListener(futureObserver)
```

Pick the `Handler`'s thread deliberately: callbacks run there, and a refresh usually does
cross-process reads (use a background handler, or hop to IO in a coroutine).

## Error handling

- Provider-side failures never crash the binder: `call()` returns an error Bundle, which
  `WorkoutDataClient` converts into a thrown `WorkoutDataException`.
- Missing write permission throws `SecurityException` (not wrapped).
- `query()` errors return an empty result to the client.
- The client sets the result Bundle's classloader **before any read** — required in
  processes that load this module through a non-default classloader (e.g. Virtual
  Active), where a premature read throws `BadParcelableException`.

## Persistence

Storage is an **in-memory** Room database behind the `WorkoutDataRepository` interface:
data survives for the life of the provider process only. To persist across restarts,
change `WorkoutDatabase.inMemory(...)` to `Room.databaseBuilder(...)` — nothing else in
the module or its callers changes.

## Tests

- `WorkoutContentProviderTest` — call()/query() contract, URI param parsing, permission
  routing, notification routing (Robolectric).
- `WorkoutDataClientTest` — end-to-end client↔provider roundtrips (URI encoding, cursor
  mapping, future).
- `RoomWorkoutDataRepositoryTest` — SQL window/downsample semantics, same-second
  override, retention trim, future lifecycle.
- `ParcelRoundTripTest` — every Parcelable through a real `Parcel`.
