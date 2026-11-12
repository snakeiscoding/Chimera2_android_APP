package com.jht.workoutdata.repository

import android.database.Cursor
import com.jht.workoutdata.model.MetricSample
import com.jht.workoutdata.model.WorkoutDataPoint
import com.jht.workoutdata.model.WorkoutFutureData
import com.jht.workoutdata.model.WorkoutMetric
import com.jht.workoutdata.query.WorkoutDataQuery

/**
 * Storage abstraction for workout history data.
 *
 * The production implementation is [RoomWorkoutDataRepository] (an in-memory
 * Room database). Because [com.jht.workoutdata.provider.WorkoutContentProvider]
 * depends only on this interface, switching Room to file-backed persistence — or
 * to a different store entirely — requires no change to the provider, its
 * contract, or any caller.
 *
 * Implementations must be safe to call from multiple binder threads.
 */
interface WorkoutDataRepository {

    /** Appends a single [point] to the history. */
    fun addPoint(point: WorkoutDataPoint)

    /** Returns the points matching [query] as materialised objects (small reads). */
    fun queryPoints(query: WorkoutDataQuery): List<WorkoutDataPoint>

    /** Returns a single [metric]'s values over time matching [query]. */
    fun querySeries(metric: WorkoutMetric, query: WorkoutDataQuery): List<MetricSample>

    /**
     * Returns a [Cursor] over the points matching [query]. Preferred for large
     * reads: the cursor streams rows via a CursorWindow in shared memory rather
     * than marshalling everything through the Binder buffer. Caller owns/closes it.
     */
    fun queryCursor(query: WorkoutDataQuery): Cursor

    /** Replaces the planned (future) workout profiles. */
    fun setFuture(future: WorkoutFutureData)

    /** Returns the current planned (future) workout profiles (empty when none). */
    fun getFuture(): WorkoutFutureData

    /** Clears all stored history AND the future profiles (call when a new workout starts). */
    fun clear()

    /** Current number of stored points. */
    fun count(): Int
}
