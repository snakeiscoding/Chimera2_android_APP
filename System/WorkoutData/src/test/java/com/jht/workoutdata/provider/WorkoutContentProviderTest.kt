package com.jht.workoutdata.provider

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jht.workoutdata.db.WorkoutDatabase
import com.jht.workoutdata.model.FutureProfile
import com.jht.workoutdata.model.FutureProfilePoint
import com.jht.workoutdata.model.WorkoutDataPoint
import com.jht.workoutdata.model.WorkoutFutureData
import com.jht.workoutdata.model.WorkoutMetric
import com.jht.workoutdata.query.WorkoutDataQuery
import com.jht.workoutdata.repository.RoomWorkoutDataRepository
import com.jht.workoutdata.repository.WorkoutDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Behaviour/contract tests for [WorkoutContentProvider]: call() semantics, error bundles,
 * URI parameter parsing, permission routing, and change-notification routing. These pin the
 * cross-process contract so storage changes can't silently break callers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorkoutContentProviderTest {

    private lateinit var context: Context
    private lateinit var provider: WorkoutContentProvider
    private lateinit var resolver: ContentResolver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context as Application).grantPermissions(WorkoutDataContract.PERMISSION_WRITE)

        provider = Robolectric.buildContentProvider(WorkoutContentProvider::class.java)
            .create(WorkoutDataContract.AUTHORITY)
            .get()
        // Replace the default repository with one whose Room DB allows main-thread queries.
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        provider.repository = RoomWorkoutDataRepository(context, db = db)
        resolver = context.contentResolver
    }

    private fun addPoint(sec: Int, hr: Int = 0): Bundle? {
        val extras = Bundle().apply {
            putParcelable(
                WorkoutDataContract.EXTRA_DATA_POINT,
                WorkoutDataPoint(elapsedTimeSec = sec, heartRate = hr),
            )
        }
        return resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_ADD_POINT, null, extras)
    }

    private fun querySeconds(uriSuffix: String = ""): List<Int> {
        val uri = android.net.Uri.parse("content://${WorkoutDataContract.AUTHORITY}/${WorkoutDataContract.PATH_POINTS}$uriSuffix")
        return resolver.query(uri, null, null, null, null)!!.use { c ->
            val idx = c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_ELAPSED_TIME_SEC)
            generateSequence { if (c.moveToNext()) c.getInt(idx) else null }.toList()
        }
    }

    // --- call() contract ---

    @Test
    fun `addPoint then query roundtrip`() {
        addPoint(1, hr = 90)
        addPoint(2, hr = 95)
        assertEquals(listOf(1, 2), querySeconds())
    }

    @Test
    fun `addPoint without extras returns error bundle, does not crash`() {
        val result = resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_ADD_POINT, null, null)
        assertNotNull(result!!.getString(WorkoutDataContract.EXTRA_ERROR))
        assertEquals(emptyList<Int>(), querySeconds())
    }

    @Test
    fun `unknown method returns error bundle`() {
        val result = resolver.call(WorkoutDataContract.CONTENT_URI, "bogusMethod", null, null)
        assertNotNull(result!!.getString(WorkoutDataContract.EXTRA_ERROR))
    }

    @Test
    fun `repository failure surfaces as error bundle, never crosses the binder as a crash`() {
        provider.repository = ThrowingRepository()
        val result = resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_CLEAR, null, null)
        assertNotNull(result!!.getString(WorkoutDataContract.EXTRA_ERROR))
    }

    // --- URI parameter parsing ---

    @Test
    fun `malformed and non-positive query params are treated as absent`() {
        (1..10).forEach { addPoint(it) }
        assertEquals(10, querySeconds("?window_sec=abc").size)   // malformed -> all
        assertEquals(10, querySeconds("?window_sec=-5").size)    // negative -> all
        assertEquals(10, querySeconds("?downsample_sec=0").size) // zero -> no downsample
    }

    @Test
    fun `window and downsample params apply`() {
        (1..10).forEach { addPoint(it) }
        // latest=10, window=4 -> since=6 (inclusive)
        assertEquals(listOf(6, 7, 8, 9, 10), querySeconds("?window_sec=4"))
        // 5s buckets over 1..10 anchored at 0: [1..4]->4, [5..9]->9, [10]->10
        assertEquals(listOf(4, 9, 10), querySeconds("?downsample_sec=5"))
    }

    @Test
    fun `from and to range params take precedence over window`() {
        (1..10).forEach { addPoint(it) }
        assertEquals(listOf(3, 4, 5), querySeconds("?from_sec=3&to_sec=5&window_sec=1"))
    }

    // --- future profile + notification routing ---

    private fun futureBundle() = Bundle().apply {
        putParcelable(
            WorkoutDataContract.EXTRA_FUTURE,
            WorkoutFutureData(
                listOf(FutureProfile(WorkoutMetric.INCLINE, listOf(FutureProfilePoint(30f, 4.5f))))
            ),
        )
    }

    @Test
    fun `setFuture then getFuture roundtrip`() {
        resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_SET_FUTURE, null, futureBundle())
        val result = resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_GET_FUTURE, null, null)!!
        result.classLoader = WorkoutFutureData::class.java.classLoader
        @Suppress("DEPRECATION")
        val future = result.getParcelable<WorkoutFutureData>(WorkoutDataContract.EXTRA_FUTURE)!!
        assertEquals(4.5f, future.profileFor(WorkoutMetric.INCLINE)!!.points.first().value)
        assertNull(future.profileFor(WorkoutMetric.SPEED))
    }

    @Test
    fun `addPoint notifies points URI only, setFuture notifies future URI only, clear notifies both`() {
        val shadow = shadowOf(resolver)
        addPoint(1)
        var notified = shadow.notifiedUris.map { it.uri.toString() }
        assertTrue(WorkoutDataContract.CONTENT_URI.toString() in notified)
        assertTrue(WorkoutDataContract.FUTURE_CONTENT_URI.toString() !in notified)

        shadow.notifiedUris.clear()
        resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_SET_FUTURE, null, futureBundle())
        notified = shadow.notifiedUris.map { it.uri.toString() }
        assertTrue(WorkoutDataContract.FUTURE_CONTENT_URI.toString() in notified)
        assertTrue(WorkoutDataContract.CONTENT_URI.toString() !in notified)

        shadow.notifiedUris.clear()
        resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_CLEAR, null, null)
        notified = shadow.notifiedUris.map { it.uri.toString() }
        assertTrue(WorkoutDataContract.CONTENT_URI.toString() in notified)
        assertTrue(WorkoutDataContract.FUTURE_CONTENT_URI.toString() in notified)
    }

    @Test
    fun `clear wipes history and future`() {
        addPoint(1)
        resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_SET_FUTURE, null, futureBundle())
        resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_CLEAR, null, null)

        assertEquals(emptyList<Int>(), querySeconds())
        val result = resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_GET_FUTURE, null, null)!!
        result.classLoader = WorkoutFutureData::class.java.classLoader
        @Suppress("DEPRECATION")
        val future = result.getParcelable<WorkoutFutureData>(WorkoutDataContract.EXTRA_FUTURE)!!
        assertEquals(0, future.profiles.size)
    }

    // --- generation token (reset detection) ---

    private fun getGeneration(): Long =
        resolver.call(
            WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_GET_GENERATION, null, null,
        )!!.getLong(WorkoutDataContract.EXTRA_GENERATION)

    @Test
    fun `generation is stable across reads and writes but changes on clear`() {
        val initial = getGeneration()
        addPoint(1)
        addPoint(2)
        assertEquals(initial, getGeneration()) // addPoint does not start a new workout

        resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_CLEAR, null, null)
        val afterClear = getGeneration()
        assertTrue(afterClear != initial)

        resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_CLEAR, null, null)
        assertTrue(getGeneration() != afterClear) // every clear is a new generation
    }

    @Test
    fun `getGeneration does not require the write permission`() {
        shadowOf(context as Application).denyPermissions(WorkoutDataContract.PERMISSION_WRITE)
        val result = resolver.call(
            WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_GET_GENERATION, null, null,
        )
        assertNotNull(result)
        assertNull(result!!.getString(WorkoutDataContract.EXTRA_ERROR))
    }

    // --- permission routing ---

    @Test
    fun `getFuture does not require the write permission`() {
        shadowOf(context as Application).denyPermissions(WorkoutDataContract.PERMISSION_WRITE)
        val result = resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_GET_FUTURE, null, null)
        assertNotNull(result) // no SecurityException, and a real bundle back
    }

    @Test(expected = SecurityException::class)
    fun `addPoint without the write permission throws`() {
        shadowOf(context as Application).denyPermissions(WorkoutDataContract.PERMISSION_WRITE)
        addPoint(1)
    }

    @Test(expected = SecurityException::class)
    fun `clear without the write permission throws`() {
        shadowOf(context as Application).denyPermissions(WorkoutDataContract.PERMISSION_WRITE)
        resolver.call(WorkoutDataContract.CONTENT_URI, WorkoutDataContract.METHOD_CLEAR, null, null)
    }

    /** Repository whose every member throws, to prove call() converts failures to error bundles. */
    private class ThrowingRepository : WorkoutDataRepository {
        override fun addPoint(point: WorkoutDataPoint) = throw IllegalStateException("boom")
        override fun queryPoints(query: WorkoutDataQuery) = throw IllegalStateException("boom")
        override fun querySeries(metric: WorkoutMetric, query: WorkoutDataQuery) =
            throw IllegalStateException("boom")
        override fun queryCursor(query: WorkoutDataQuery) = throw IllegalStateException("boom")
        override fun setFuture(future: WorkoutFutureData) = throw IllegalStateException("boom")
        override fun getFuture() = throw IllegalStateException("boom")
        override fun clear() = throw IllegalStateException("boom")
        override fun count() = throw IllegalStateException("boom")
    }
}
