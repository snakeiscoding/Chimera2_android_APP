package com.jht.workoutdata.query

/**
 * Describes how much of the workout history to return and at what resolution.
 *
 * The time axis is [com.jht.workoutdata.model.WorkoutDataPoint.elapsedTimeSec]
 * (whole seconds of workout time), so both knobs are expressed in seconds. They
 * compose freely, covering every case the consumer asked for:
 *  - all data            -> [WorkoutDataQuery.ALL]
 *  - last 30 minutes     -> `WorkoutDataQuery.lastMinutes(30)`
 *  - every 5 seconds     -> `WorkoutDataQuery.everySeconds(5)`
 *  - every 1 minute      -> `WorkoutDataQuery.everyMinutes(1)`
 *  - last 30 min @ 5s    -> `WorkoutDataQuery.lastMinutes(30, 5)`
 *
 * @param windowSec Keep only points newer than `latestElapsedSec - windowSec`.
 *   `null` (or <= 0) means no window — return everything.
 * @param downsampleSec Bucket size (seconds) for down-sampling; one point (the
 *   latest in the bucket) is emitted per bucket. `null` (or <= 0) means no
 *   down-sampling. Fully customizable: pass any interval (5 s, 60 s, 600 s, …).
 * @param fromSec Absolute inclusive lower bound on elapsed seconds. When either
 *   range bound is set, the range wins and [windowSec]/[downsampleSec] are
 *   ignored — a ranged read is an incremental append or a gap probe, never a
 *   decimated view.
 * @param toSec Absolute inclusive upper bound; see [fromSec].
 */
data class WorkoutDataQuery(
    val windowSec: Int? = null,
    val downsampleSec: Int? = null,
    val fromSec: Int? = null,
    val toSec: Int? = null,
) {
    val hasWindow: Boolean get() = (windowSec ?: 0) > 0
    val hasDownsample: Boolean get() = (downsampleSec ?: 0) > 0

    /** True when an absolute [fromSec]/[toSec] range is set (takes precedence over window). */
    val hasRange: Boolean get() = fromSec != null || toSec != null

    companion object {
        /** Returns the full, unfiltered history. Java: `WorkoutDataQuery.ALL`. */
        @JvmField
        val ALL = WorkoutDataQuery()

        /** Absolute inclusive range on elapsed seconds (for incremental / gap reads). */
        @JvmStatic
        fun between(fromSec: Int, toSec: Int) = WorkoutDataQuery(fromSec = fromSec, toSec = toSec)

        /**
         * Everything at or after [fromSec], open-ended. The idiomatic incremental read:
         * `since(lastSeenSec + 1)` returns all points newer than what the caller already has.
         */
        @JvmStatic
        fun since(fromSec: Int) = WorkoutDataQuery(fromSec = fromSec)

        /** Last [seconds] of history, optionally down-sampled to [downsampleSec]. */
        @JvmStatic
        @JvmOverloads
        fun lastSeconds(seconds: Int, downsampleSec: Int? = null) =
            WorkoutDataQuery(windowSec = seconds, downsampleSec = downsampleSec)

        /** Last [minutes] of history, optionally down-sampled to [downsampleSec]. */
        @JvmStatic
        @JvmOverloads
        fun lastMinutes(minutes: Int, downsampleSec: Int? = null) =
            WorkoutDataQuery(windowSec = minutes * 60, downsampleSec = downsampleSec)

        /** All history, down-sampled to one point every [s] seconds. */
        @JvmStatic
        @JvmOverloads
        fun everySeconds(s: Int, windowSec: Int? = null) =
            WorkoutDataQuery(windowSec = windowSec, downsampleSec = s)

        /** All history, down-sampled to one point every [m] minutes. */
        @JvmStatic
        @JvmOverloads
        fun everyMinutes(m: Int, windowSec: Int? = null) =
            WorkoutDataQuery(windowSec = windowSec, downsampleSec = m * 60)
    }
}
