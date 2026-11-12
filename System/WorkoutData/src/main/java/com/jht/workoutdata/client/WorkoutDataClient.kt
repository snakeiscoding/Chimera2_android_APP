package com.jht.workoutdata.client

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import com.jht.workoutdata.model.MetricSample
import com.jht.workoutdata.model.WorkoutDataPoint
import com.jht.workoutdata.model.WorkoutFutureData
import com.jht.workoutdata.model.WorkoutMetric
import com.jht.workoutdata.provider.WorkoutDataContract
import com.jht.workoutdata.provider.getParcelableCompat
import com.jht.workoutdata.query.WorkoutDataQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Typed, ergonomic wrapper around [WorkoutContentProvider].
 *
 * Hides the URIs, query parameters, Cursor row-mapping, and call() marshalling
 * so callers work with [WorkoutDataPoint] / [MetricSample] directly.
 *
 * Reads cross the binder boundary as a Cursor (genuine IPC), so the suspend read
 * functions run on [ioDispatcher]. Writes ([addPoint]) are a single small
 * transaction and exposed synchronously for the polling hot-path; call from a
 * background thread if you poll on the main thread.
 *
 * A [WorkoutDataException] is thrown if the provider reports an error or is
 * unreachable.
 */
class WorkoutDataClient @JvmOverloads constructor(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    /** Appends a single point. Synchronous; safe to call off the main thread. */
    fun addPoint(point: WorkoutDataPoint) {
        val extras = Bundle().apply { putParcelable(WorkoutDataContract.EXTRA_DATA_POINT, point) }
        callOrThrow(WorkoutDataContract.METHOD_ADD_POINT, extras)
    }

    // --- Kotlin (suspend) API: hops to ioDispatcher automatically. ---

    /** Clears all stored history. Call this when a new workout starts. */
    suspend fun clear() = withContext(ioDispatcher) { clearInternal() }

    /**
     * Returns full points matching [query]. Backed by a Cursor, so this is safe
     * for large results (e.g. an entire session's raw history).
     */
    suspend fun queryPoints(query: WorkoutDataQuery = WorkoutDataQuery.ALL): List<WorkoutDataPoint> =
        withContext(ioDispatcher) { queryPointsInternal(query) }

    /** Returns a single [metric]'s time series matching [query]. */
    suspend fun querySeries(
        metric: WorkoutMetric,
        query: WorkoutDataQuery = WorkoutDataQuery.ALL,
    ): List<MetricSample> = withContext(ioDispatcher) { querySeriesInternal(metric, query) }

    // --- Java (blocking) API: runs on the calling thread, which MUST be a ---
    // --- background thread (these perform cross-process IPC). ---

    /** Blocking [clear] for Java callers. Call off the main thread. */
    fun clearBlocking() = clearInternal()

    /** Blocking [queryPoints] for Java callers. Call off the main thread. */
    @JvmOverloads
    fun queryPointsBlocking(query: WorkoutDataQuery = WorkoutDataQuery.ALL): List<WorkoutDataPoint> =
        queryPointsInternal(query)

    /**
     * Blocking read of points in the inclusive elapsed-second range [fromSec, toSec]
     * (e.g. single-second gap backfill with fromSec == toSec). Call off the main thread.
     */
    fun queryPointsBetweenBlocking(fromSec: Int, toSec: Int): List<WorkoutDataPoint> =
        queryPointsInternal(WorkoutDataQuery.between(fromSec, toSec))

    /**
     * Blocking read of all points at or after [fromSec], open-ended. The idiomatic
     * incremental read: `queryPointsSinceBlocking(lastSeenSec + 1)` returns everything
     * newer than what the caller already holds. Call off the main thread.
     */
    fun queryPointsSinceBlocking(fromSec: Int): List<WorkoutDataPoint> =
        queryPointsInternal(WorkoutDataQuery.since(fromSec))

    /** Blocking [querySeries] for Java callers. Call off the main thread. */
    @JvmOverloads
    fun querySeriesBlocking(
        metric: WorkoutMetric,
        query: WorkoutDataQuery = WorkoutDataQuery.ALL,
    ): List<MetricSample> = querySeriesInternal(metric, query)

    // --- Shared synchronous implementations ---

    private fun clearInternal() {
        callOrThrow(WorkoutDataContract.METHOD_CLEAR, null)
    }

    private fun queryPointsInternal(query: WorkoutDataQuery): List<WorkoutDataPoint> =
        openCursor(query)?.use { cursor ->
            ArrayList<WorkoutDataPoint>(cursor.count).apply {
                val m = CursorColumns(cursor)
                while (cursor.moveToNext()) add(m.readPoint(cursor))
            }
        } ?: emptyList()

    private fun querySeriesInternal(
        metric: WorkoutMetric,
        query: WorkoutDataQuery,
    ): List<MetricSample> =
        openCursor(query)?.use { cursor ->
            val elapsedIdx = cursor.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_ELAPSED_TIME_SEC)
            val valueIdx = cursor.getColumnIndexOrThrow(metric.columnName)
            ArrayList<MetricSample>(cursor.count).apply {
                while (cursor.moveToNext()) {
                    add(
                        MetricSample(
                            elapsedTimeSec = cursor.getInt(elapsedIdx),
                            value = cursor.getFloat(valueIdx), // getFloat is valid for int columns too
                        )
                    )
                }
            }
        } ?: emptyList()

    /**
     * Returns a raw [Cursor] over the matching points for advanced consumers that
     * want to stream/page very large results themselves (the caller must close it).
     * Most callers should use [queryPoints] or [querySeries] instead.
     */
    fun openPointsCursor(query: WorkoutDataQuery = WorkoutDataQuery.ALL): Cursor? = openCursor(query)

    // --- Future profile ---

    /**
     * Replaces the planned (future) workout profiles. Synchronous, one small
     * transaction; safe to call off the main thread. Requires the write permission.
     */
    fun setFuture(future: WorkoutFutureData) {
        val extras = Bundle().apply { putParcelable(WorkoutDataContract.EXTRA_FUTURE, future) }
        callOrThrow(WorkoutDataContract.METHOD_SET_FUTURE, extras)
    }

    /** Blocking read of the current future profiles (empty when none). Call off the main thread. */
    fun getFutureBlocking(): WorkoutFutureData {
        // callOrThrow sets the Bundle classloader before any read (see note there).
        val result = callOrThrow(WorkoutDataContract.METHOD_GET_FUTURE, null)
        return result.getParcelableCompat(
            WorkoutDataContract.EXTRA_FUTURE, WorkoutFutureData::class.java,
        ) ?: WorkoutFutureData()
    }

    /** Suspend variant of [getFutureBlocking] for Kotlin callers. */
    suspend fun getFuture(): WorkoutFutureData = withContext(ioDispatcher) { getFutureBlocking() }

    /**
     * Blocking read of the store's generation token: changes on every clear and on
     * provider restart, otherwise stable. Incremental readers compare it between reads
     * to detect that their cached history belongs to an earlier workout (elapsed-second
     * continuity alone cannot, once the new workout's clock passes the old one).
     * Call off the main thread.
     */
    fun getGenerationBlocking(): Long =
        callOrThrow(WorkoutDataContract.METHOD_GET_GENERATION, null)
            .getLong(WorkoutDataContract.EXTRA_GENERATION)

    // --- Change notifications ---

    /**
     * Registers [onChange] to run (on [handler]'s thread) whenever the workout HISTORY
     * changes — a point is added (~1/sec during a workout) or the history is cleared.
     * Future-profile changes do NOT fire this; use [registerFutureChangeListener].
     * Works across processes; a provider-backed replacement for graph-update events.
     *
     * @return the observer; pass it to [unregisterChangeListener] when done listening.
     */
    fun registerChangeListener(handler: Handler, onChange: Runnable): ContentObserver =
        registerObserver(WorkoutDataContract.CONTENT_URI, handler, onChange)

    /**
     * Registers [onChange] to run (on [handler]'s thread) whenever the planned FUTURE
     * profiles change — [setFuture] was called or the workout was cleared. Fires only
     * when the future actually changes (rare), not per history point.
     */
    fun registerFutureChangeListener(handler: Handler, onChange: Runnable): ContentObserver =
        registerObserver(WorkoutDataContract.FUTURE_CONTENT_URI, handler, onChange)

    /** Unregisters an observer returned by either register method. */
    fun unregisterChangeListener(observer: ContentObserver) {
        resolver.unregisterContentObserver(observer)
    }

    private fun registerObserver(uri: Uri, handler: Handler, onChange: Runnable): ContentObserver {
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                onChange.run()
            }
        }
        resolver.registerContentObserver(uri, true, observer)
        return observer
    }

    private fun openCursor(query: WorkoutDataQuery): Cursor? {
        val uri = buildUri(query)
        return try {
            resolver.query(uri, null, null, null, null)
        } catch (t: Throwable) {
            throw WorkoutDataException("Failed to query WorkoutData provider", t)
        }
    }

    private fun buildUri(query: WorkoutDataQuery): Uri =
        WorkoutDataContract.CONTENT_URI.buildUpon().apply {
            query.windowSec?.let {
                appendQueryParameter(WorkoutDataContract.QUERY_PARAM_WINDOW_SEC, it.toString())
            }
            query.downsampleSec?.let {
                appendQueryParameter(WorkoutDataContract.QUERY_PARAM_DOWNSAMPLE_SEC, it.toString())
            }
            query.fromSec?.let {
                appendQueryParameter(WorkoutDataContract.QUERY_PARAM_FROM_SEC, it.toString())
            }
            query.toSec?.let {
                appendQueryParameter(WorkoutDataContract.QUERY_PARAM_TO_SEC, it.toString())
            }
        }.build()

    private fun callOrThrow(method: String, extras: Bundle?): Bundle {
        val result = try {
            resolver.call(WorkoutDataContract.CONTENT_URI, method, null, extras)
        } catch (t: Throwable) {
            throw WorkoutDataException("Failed to reach WorkoutData provider for '$method'", t)
        } ?: throw WorkoutDataException("WorkoutData provider returned null for '$method'")

        // MUST be set before ANY read: the first key access unparcels the entire Bundle, and in
        // processes that load this module through a non-default classloader (e.g. Virtual Active's
        // reflection-loaded integration) the default loader cannot resolve our Parcelables,
        // throwing BadParcelableException. Use this class's loader — it resolves the whole module.
        result.classLoader = WorkoutDataClient::class.java.classLoader

        result.getString(WorkoutDataContract.EXTRA_ERROR)?.let { error ->
            throw WorkoutDataException("WorkoutData provider error on '$method': $error")
        }
        return result
    }
}

/**
 * Resolves and caches every column index once per Cursor, then reads full points
 * by index (cheaper than a name lookup per row per column).
 */
private class CursorColumns(c: Cursor) {
    private val elapsedTimeSec = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_ELAPSED_TIME_SEC)
    private val workoutStage = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_WORKOUT_STAGE)
    private val lapIndex = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_LAP_INDEX)
    private val splitIndex = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_SPLIT_INDEX)
    private val distanceKm = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_DISTANCE_KM)
    private val speedKph = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_SPEED_KPH)
    private val inclinePercent = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_INCLINE_PERCENT)
    private val resistance = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_RESISTANCE)
    private val rpm = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_RPM)
    private val spm = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_SPM)
    private val heartRate = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_HEART_RATE)
    private val watts = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_WATTS)
    private val targetHeartRate = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_TARGET_HEART_RATE)
    private val targetWatts = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_TARGET_WATTS)
    private val totalCalories = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_TOTAL_CALORIES)
    private val mets = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_METS)
    private val totalFloors = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_TOTAL_FLOORS)
    private val totalSteps = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_TOTAL_STEPS)
    private val elevationMeters = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_ELEVATION_METERS)

    fun readPoint(c: Cursor) = WorkoutDataPoint(
        elapsedTimeSec = c.getInt(elapsedTimeSec),
        workoutStage = c.getInt(workoutStage),
        lapIndex = c.getInt(lapIndex),
        splitIndex = c.getInt(splitIndex),
        distanceKm = c.getFloat(distanceKm),
        speedKph = c.getFloat(speedKph),
        inclinePercent = c.getFloat(inclinePercent),
        resistance = c.getInt(resistance),
        rpm = c.getInt(rpm),
        spm = c.getInt(spm),
        heartRate = c.getInt(heartRate),
        watts = c.getInt(watts),
        targetHeartRate = c.getInt(targetHeartRate),
        targetWatts = c.getInt(targetWatts),
        totalCalories = c.getFloat(totalCalories),
        mets = c.getFloat(mets),
        totalFloors = c.getFloat(totalFloors),
        totalSteps = c.getInt(totalSteps),
        elevationMeters = c.getFloat(elevationMeters),
    )
}

/** Thrown when the WorkoutData provider is unreachable or reports an error. */
class WorkoutDataException(message: String, cause: Throwable? = null) : Exception(message, cause)
