package com.jht.interfaces.models

import com.jht.workoutdata.model.WorkoutDataPoint
import java.util.TreeMap

/**
 * The sliding-window snapshot state machine backing the workout graphs.
 *
 * Extracted from MatrixWorkoutModel so the logic is unit-testable against a fake [Source]:
 *  - bulk-loads the window once (first refresh or after a reset),
 *  - then appends only points newer than the last seen second,
 *  - backfills single-second gaps (probing each missing second once; genuinely empty seconds
 *    are cached in [knownEmptySeconds] and never re-requested),
 *  - detects a workout reset via the store's generation token (which changes on every clear
 *    and on provider restart) and bulk-reloads — elapsed-second continuity alone cannot
 *    detect a reset once the new workout's clock passes the old one,
 *  - evicts everything older than the sliding window so the snapshot never exceeds the
 *    window (at most [WorkoutGraphConfig.windowSeconds] + 1 entries — both ends inclusive).
 *
 * Not thread-safe by itself: callers must serialise [refresh] calls (the model runs it inside
 * a conflated coroutine on the IO dispatcher).
 */
class GraphSnapshotManager(
    private val source: Source,
    private val windowMinutes: () -> Int = { WorkoutGraphConfig.windowMinutes },
    private val windowSeconds: () -> Int = { WorkoutGraphConfig.windowSeconds },
) {

    /** The reads the manager needs; implemented against WorkoutDataClient in production. */
    interface Source {
        /** All points in the last [minutes] minutes, ascending by elapsed second. */
        fun window(minutes: Int): List<WorkoutDataPoint>

        /** All points at or after [fromSec], open-ended, ascending (incremental appends). */
        fun since(fromSec: Int): List<WorkoutDataPoint>

        /** Points in the inclusive elapsed-second range, ascending (gap backfill). */
        fun between(fromSec: Int, toSec: Int): List<WorkoutDataPoint>

        /** The store's generation token: changes on every clear/restart, otherwise stable. */
        fun generation(): Long
    }

    // History keyed by elapsed second: sorted, and de-duplicates multiple rows per second.
    private val graphPoints = TreeMap<Int, WorkoutDataPoint>()
    private val knownEmptySeconds = HashSet<Int>()
    private var lastAppendedSec = -1
    private var lastGeneration: Long? = null

    /** The current snapshot, ascending by elapsed second. */
    @Volatile
    var snapshot: List<WorkoutDataPoint> = emptyList()
        private set

    /**
     * Refreshes the snapshot: bulk load on first use or reset, incremental append + gap
     * backfill + eviction otherwise. On failure the previous snapshot is kept.
     */
    fun refresh() {
        // Read the generation BEFORE the data reads: if a clear lands in between, the stale
        // token forces a bulk reload on the next refresh instead of leaving spliced data.
        val generation = source.generation()
        if (graphPoints.isEmpty() || generation != lastGeneration) {
            bulkLoad()
            lastGeneration = generation
            return
        }
        val newPoints = source.since(lastAppendedSec + 1)
        if (newPoints.isNotEmpty()) {
            newPoints.forEach { graphPoints[it.elapsedTimeSec] = it }
            lastAppendedSec = graphPoints.lastKey()
        }
        backfillSingleSecondGaps()
        evictOutsideWindow()
        snapshot = graphPoints.values.toList()
    }

    private fun bulkLoad() {
        val bulk = source.window(windowMinutes())
        graphPoints.clear()
        knownEmptySeconds.clear()
        bulk.forEach { graphPoints[it.elapsedTimeSec] = it }
        lastAppendedSec = if (graphPoints.isEmpty()) -1 else graphPoints.lastKey()
        snapshot = graphPoints.values.toList()
    }

    /**
     * Requests any single missing second inside the window and merges the result.
     *
     * A missing second normally means the writer's 1Hz tick skipped it (timer jitter) or
     * its addPoint failed — such seconds usually never fill, so each gap is probed at most
     * once and then cached in [knownEmptySeconds]. Only single-second gaps are probed:
     * a wider gap means the writer was gated (paused / outside the workout stage), and
     * re-probing whole ranges every tick would be wasted IPC for data that isn't coming.
     */
    private fun backfillSingleSecondGaps() {
        if (graphPoints.size < 2) return
        val windowStart = graphPoints.lastKey() - windowSeconds()
        val keys = graphPoints.keys.toIntArray() // ascending (TreeMap)
        val missing = ArrayList<Int>()
        for (i in 0 until keys.size - 1) {
            val gap = keys[i] + 1
            // exactly one second missing between stored seconds, inside the window, not known-empty
            if (keys[i + 1] - keys[i] == 2 && gap >= windowStart && gap !in knownEmptySeconds) {
                missing.add(gap)
            }
        }
        for (sec in missing) {
            val found = source.between(sec, sec)
            if (found.isEmpty()) {
                knownEmptySeconds.add(sec) // never recorded; don't ask again
            } else {
                found.forEach { graphPoints[it.elapsedTimeSec] = it }
            }
        }
    }

    /** Caps the snapshot to the sliding window by dropping the oldest seconds. */
    private fun evictOutsideWindow() {
        if (graphPoints.isEmpty()) return
        // windowStart itself is kept (strict <), so the window is inclusive on both ends:
        // at most windowSeconds + 1 entries survive.
        val windowStart = graphPoints.lastKey() - windowSeconds()
        while (graphPoints.isNotEmpty() && graphPoints.firstKey() < windowStart) {
            graphPoints.pollFirstEntry()
        }
        knownEmptySeconds.removeAll { it < windowStart }
    }
}
