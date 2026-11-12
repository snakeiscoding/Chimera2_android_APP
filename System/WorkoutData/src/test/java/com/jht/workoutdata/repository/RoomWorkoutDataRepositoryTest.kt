package com.jht.workoutdata.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jht.workoutdata.db.WorkoutDatabase
import com.jht.workoutdata.model.WorkoutDataPoint
import com.jht.workoutdata.model.WorkoutMetric
import com.jht.workoutdata.provider.WorkoutDataContract
import com.jht.workoutdata.query.WorkoutDataQuery
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the real SQL window/down-sample logic (on the elapsed-second axis)
 * against an in-memory SQLite database via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomWorkoutDataRepositoryTest {

    private lateinit var db: WorkoutDatabase
    private lateinit var repo: RoomWorkoutDataRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoomWorkoutDataRepository(ctx, db = db)
    }

    @After
    fun tearDown() = db.close()

    private fun point(sec: Int, hr: Int = 0) =
        WorkoutDataPoint(elapsedTimeSec = sec, heartRate = hr)

    private fun add(vararg secs: Int) = secs.forEach { repo.addPoint(point(it)) }

    private fun elapsed(query: WorkoutDataQuery) =
        repo.queryPoints(query).map { it.elapsedTimeSec }

    @Test
    fun `query ALL returns everything ascending`() {
        repo.addPoint(point(3))
        repo.addPoint(point(1))
        repo.addPoint(point(2))
        assertEquals(listOf(1, 2, 3), elapsed(WorkoutDataQuery.ALL))
    }

    @Test
    fun `empty store returns empty`() {
        assertEquals(emptyList<Int>(), elapsed(WorkoutDataQuery.ALL))
        assertEquals(emptyList<Int>(), elapsed(WorkoutDataQuery.lastMinutes(30)))
    }

    @Test
    fun `window keeps points within range of the latest point`() {
        add(10, 70, 80, 100)
        // latest=100s, window=30s -> since=70 (inclusive)
        assertEquals(listOf(70, 80, 100), elapsed(WorkoutDataQuery.lastSeconds(30)))
    }

    @Test
    fun `downsample keeps the latest point per bucket`() {
        add(0, 1, 2, 3, 4, 5, 6, 7)
        // 5s buckets: [0..4]->4, [5..7]->7
        assertEquals(listOf(4, 7), elapsed(WorkoutDataQuery.everySeconds(5)))
    }

    @Test
    fun `window and downsample compose`() {
        add(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        // last 6s -> since=4; 2s buckets of {4..10}
        assertEquals(listOf(5, 7, 9, 10), elapsed(WorkoutDataQuery.lastSeconds(6, 2)))
    }

    @Test
    fun `querySeries projects a single metric`() {
        repo.addPoint(point(1, hr = 100))
        repo.addPoint(point(2, hr = 110))
        val series = repo.querySeries(WorkoutMetric.HEART_RATE, WorkoutDataQuery.ALL)
        assertEquals(listOf(100f, 110f), series.map { it.value })
        assertEquals(listOf(1, 2), series.map { it.elapsedTimeSec })
    }

    @Test
    fun `same second overrides instead of duplicating - last write wins`() {
        repo.addPoint(point(5, hr = 100))
        repo.addPoint(point(5, hr = 120)) // duplicate second (e.g. poll-timer jitter)

        assertEquals(1, repo.count())
        val stored = repo.queryPoints(WorkoutDataQuery.ALL)
        assertEquals(listOf(5), stored.map { it.elapsedTimeSec })
        assertEquals(120, stored.first().heartRate) // latest write kept
    }

    @Test
    fun `downsample returns exactly one row per bucket even after same-second rewrites`() {
        add(0, 1, 2, 3, 4)
        repo.addPoint(point(4, hr = 77)) // rewrite the bucket's max second
        val result = repo.queryPoints(WorkoutDataQuery.everySeconds(5))
        assertEquals(listOf(4), result.map { it.elapsedTimeSec }) // no tie-leak duplicates
        assertEquals(77, result.first().heartRate)
    }

    @Test
    fun `clear empties the store`() {
        add(1, 2)
        repo.clear()
        assertEquals(0, repo.count())
        assertEquals(emptyList<Int>(), elapsed(WorkoutDataQuery.ALL))
    }

    @Test
    fun `retention guard caps row count and drops oldest`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val capped = RoomWorkoutDataRepository(ctx, maxRows = 3, db = db)
        (0..4).forEach { capped.addPoint(point(it)) }
        assertEquals(3, capped.count())
        assertEquals(listOf(2, 3, 4), capped.queryPoints(WorkoutDataQuery.ALL).map { it.elapsedTimeSec })
    }

    @Test
    fun `queryCursor exposes contract columns`() {
        repo.addPoint(point(1, hr = 95))
        repo.queryCursor(WorkoutDataQuery.ALL).use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            val sec = c.getInt(c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_ELAPSED_TIME_SEC))
            val hr = c.getInt(c.getColumnIndexOrThrow(WorkoutDataContract.COLUMN_HEART_RATE))
            assertEquals(1, sec)
            assertEquals(95, hr)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-positive capacity is rejected`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        RoomWorkoutDataRepository(ctx, maxRows = 0, db = db)
    }

    @Test
    fun `future set, get and clear`() {
        val future = com.jht.workoutdata.model.WorkoutFutureData(
            listOf(
                com.jht.workoutdata.model.FutureProfile(
                    WorkoutMetric.INCLINE,
                    listOf(com.jht.workoutdata.model.FutureProfilePoint(timeSec = 30f, value = 4.5f)),
                )
            )
        )
        repo.setFuture(future)

        val stored = repo.getFuture()
        assertEquals(1, stored.profiles.size)
        assertEquals(4.5f, stored.profileFor(WorkoutMetric.INCLINE)!!.points.first().value)
        assertEquals(null, stored.profileFor(WorkoutMetric.SPEED)) // absent metric -> null

        repo.clear() // clearing the workout wipes the future too
        assertEquals(0, repo.getFuture().profiles.size)
    }
}
