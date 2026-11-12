package com.jht.workoutdata.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.jht.workoutdata.model.WorkoutDataPoint
import com.jht.workoutdata.model.WorkoutFutureData
import com.jht.workoutdata.query.WorkoutDataQuery
import com.jht.workoutdata.repository.RoomWorkoutDataRepository
import com.jht.workoutdata.repository.WorkoutDataRepository
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "WorkoutContentProvider"

/**
 * Exposes workout history across process boundaries.
 *
 * Reads go through [query]: a [Cursor] over the matching points, with the time
 * window and down-sample interval supplied as query parameters on the [Uri]
 * (see [WorkoutDataContract]). Cursor results stream via a CursorWindow in
 * shared memory, so even a full-history read is not capped by the Binder
 * transaction limit.
 *
 * Writes and lifecycle go through [call]:
 *  - [WorkoutDataContract.METHOD_ADD_POINT]      — append one [WorkoutDataPoint]
 *  - [WorkoutDataContract.METHOD_CLEAR]          — drop all history (new workout)
 *  - [WorkoutDataContract.METHOD_GET_GENERATION] — read the reset-detection token
 *
 * Prefer the typed `WorkoutDataClient` over calling this directly.
 *
 * The provider is unaware of equipment frame types: callers push only the fields
 * relevant to their machine. Storage is delegated to a [WorkoutDataRepository].
 */
class WorkoutContentProvider : ContentProvider() {

    // Visible for testing so a fake/injected repository can be supplied.
    internal var repository: WorkoutDataRepository? = null

    // Generation token for reset detection (see METHOD_GET_GENERATION in the contract).
    // Seeded from wall-clock so a provider-process restart (which also empties the
    // in-memory store) reads as a new generation; incremented on every clear.
    private val generation = AtomicLong(System.currentTimeMillis())

    private val repo: WorkoutDataRepository
        get() = repository ?: error("WorkoutContentProvider not initialised")

    override fun onCreate(): Boolean {
        if (repository == null) {
            val ctx = context ?: return false
            repository = RoomWorkoutDataRepository(ctx)
        }
        return true
    }

    // --- Reads: Cursor over shared memory ---

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        return try {
            val cursor = repo.queryCursor(queryFromUri(uri))
            context?.contentResolver?.let { cursor.setNotificationUri(it, uri) }
            cursor
        } catch (t: Throwable) {
            Log.e(TAG, "query($uri) failed", t)
            null
        }
    }

    private fun queryFromUri(uri: Uri): WorkoutDataQuery {
        val windowSec = uri.getQueryParameter(WorkoutDataContract.QUERY_PARAM_WINDOW_SEC)
            ?.toIntOrNull()?.takeIf { it > 0 }
        val downsampleSec = uri.getQueryParameter(WorkoutDataContract.QUERY_PARAM_DOWNSAMPLE_SEC)
            ?.toIntOrNull()?.takeIf { it > 0 }
        // Absolute range (may be 0 or negative bounds, so no >0 filter).
        val fromSec = uri.getQueryParameter(WorkoutDataContract.QUERY_PARAM_FROM_SEC)?.toIntOrNull()
        val toSec = uri.getQueryParameter(WorkoutDataContract.QUERY_PARAM_TO_SEC)?.toIntOrNull()
        return WorkoutDataQuery(
            windowSec = windowSec,
            downsampleSec = downsampleSec,
            fromSec = fromSec,
            toSec = toSec,
        )
    }

    // --- Writes / lifecycle: call() ---

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        // Mutating methods require the write permission; reads (getFuture, getGeneration)
        // are open like query(). Enforced in code because the framework does not apply
        // android:writePermission to call(). Thrown before the catch below so the
        // SecurityException propagates to the caller rather than becoming an error Bundle.
        when (method) {
            WorkoutDataContract.METHOD_ADD_POINT,
            WorkoutDataContract.METHOD_SET_FUTURE,
            WorkoutDataContract.METHOD_CLEAR -> enforceWritePermission()
        }
        return try {
            when (method) {
                WorkoutDataContract.METHOD_ADD_POINT -> handleAddPoint(extras)
                WorkoutDataContract.METHOD_SET_FUTURE -> handleSetFuture(extras)
                WorkoutDataContract.METHOD_GET_FUTURE -> handleGetFuture()
                WorkoutDataContract.METHOD_GET_GENERATION -> handleGetGeneration()
                WorkoutDataContract.METHOD_CLEAR -> handleClear()
                else -> {
                    Log.w(TAG, "Unsupported call method: $method")
                    errorBundle("Unsupported method: $method")
                }
            }
        } catch (t: Throwable) {
            // Never let an exception cross the binder boundary as a crash.
            Log.e(TAG, "call($method) failed", t)
            errorBundle(t.message ?: t.javaClass.simpleName)
        }
    }

    private fun handleAddPoint(extras: Bundle?): Bundle {
        if (extras == null) return errorBundle("Missing extras for addPoint")
        extras.classLoader = WorkoutDataPoint::class.java.classLoader
        val point = extras.getParcelableCompat(
            WorkoutDataContract.EXTRA_DATA_POINT,
            WorkoutDataPoint::class.java,
        ) ?: return errorBundle("Missing ${WorkoutDataContract.EXTRA_DATA_POINT}")

        repo.addPoint(point)
        notifyHistoryChange()
        return Bundle().apply { putInt(WorkoutDataContract.EXTRA_RESULT_COUNT, 1) }
    }

    private fun handleSetFuture(extras: Bundle?): Bundle {
        if (extras == null) return errorBundle("Missing extras for setFuture")
        extras.classLoader = WorkoutFutureData::class.java.classLoader
        val future = extras.getParcelableCompat(
            WorkoutDataContract.EXTRA_FUTURE,
            WorkoutFutureData::class.java,
        ) ?: return errorBundle("Missing ${WorkoutDataContract.EXTRA_FUTURE}")

        repo.setFuture(future)
        notifyFutureChange()
        return Bundle().apply { putInt(WorkoutDataContract.EXTRA_RESULT_COUNT, future.profiles.size) }
    }

    private fun handleGetFuture(): Bundle = Bundle().apply {
        putParcelable(WorkoutDataContract.EXTRA_FUTURE, repo.getFuture())
    }

    private fun handleGetGeneration(): Bundle = Bundle().apply {
        putLong(WorkoutDataContract.EXTRA_GENERATION, generation.get())
    }

    private fun handleClear(): Bundle {
        repo.clear()
        // Bump the generation only AFTER the store is cleared: a reader that observes the
        // new token must never be able to read pre-clear data, or it would cache the old
        // workout's points under the new generation and the reset would go undetected.
        // (The opposite race — old token, post-clear data — self-heals on the reader's
        // next refresh because the token no longer matches.)
        generation.incrementAndGet()
        // Clearing wipes both history and the future; notify both URIs.
        notifyHistoryChange()
        notifyFutureChange()
        return Bundle().apply { putInt(WorkoutDataContract.EXTRA_RESULT_COUNT, 0) }
    }

    private fun notifyHistoryChange() {
        context?.contentResolver?.notifyChange(WorkoutDataContract.CONTENT_URI, null)
    }

    private fun notifyFutureChange() {
        context?.contentResolver?.notifyChange(WorkoutDataContract.FUTURE_CONTENT_URI, null)
    }

    /**
     * Throws [SecurityException] unless the caller holds [WorkoutDataContract.PERMISSION_WRITE].
     * Uses checkCallingOrSelf so in-process writers (the host app, which holds the
     * permission) pass, while a cross-process caller is checked against its own grant.
     */
    private fun enforceWritePermission() {
        val ctx = context ?: throw SecurityException("WorkoutData provider has no context")
        if (ctx.checkCallingOrSelfPermission(WorkoutDataContract.PERMISSION_WRITE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException(
                "Writing WorkoutData requires ${WorkoutDataContract.PERMISSION_WRITE}"
            )
        }
    }

    private fun errorBundle(message: String): Bundle =
        Bundle().apply { putString(WorkoutDataContract.EXTRA_ERROR, message) }

    // --- Unused Cursor-style mutators: this provider writes via call(). ---

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Use call(METHOD_ADD_POINT) / WorkoutDataClient.addPoint()")

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("WorkoutData history is append-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Use call(METHOD_CLEAR) / WorkoutDataClient.clear()")

    override fun getType(uri: Uri): String? = null
}
