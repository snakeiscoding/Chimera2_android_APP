package com.jht.workoutdata.client

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jht.workoutdata.db.WorkoutDatabase
import com.jht.workoutdata.model.FutureProfile
import com.jht.workoutdata.model.FutureProfilePoint
import com.jht.workoutdata.model.WorkoutDataPoint
import com.jht.workoutdata.model.WorkoutFutureData
import com.jht.workoutdata.model.WorkoutMetric
import com.jht.workoutdata.provider.WorkoutContentProvider
import com.jht.workoutdata.provider.WorkoutDataContract
import com.jht.workoutdata.query.WorkoutDataQuery
import com.jht.workoutdata.repository.RoomWorkoutDataRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * End-to-end tests of [WorkoutDataClient] against the real provider: exercises URI parameter
 * encoding (buildUri), Cursor row mapping (CursorColumns), and the call() marshalling — the
 * pieces a unit test of either side alone would miss.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorkoutDataClientTest {

    private lateinit var client: WorkoutDataClient

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context as Application).grantPermissions(WorkoutDataContract.PERMISSION_WRITE)
        val provider = Robolectric.buildContentProvider(WorkoutContentProvider::class.java)
            .create(WorkoutDataContract.AUTHORITY)
            .get()
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        provider.repository = RoomWorkoutDataRepository(context, db = db)
        client = WorkoutDataClient(context)
    }

    @Test
    fun `full point roundtrip preserves every field through cursor mapping`() {
        val point = WorkoutDataPoint(
            elapsedTimeSec = 61, workoutStage = 1, lapIndex = 2, splitIndex = 3,
            distanceKm = 1.25f, speedKph = 8.5f, inclinePercent = 3.5f, resistance = 7,
            rpm = 85, spm = 120, heartRate = 132, watts = 210, targetHeartRate = 140,
            targetWatts = 220, totalCalories = 55.5f, mets = 6.2f, totalFloors = 2.5f,
            totalSteps = 640, elevationMeters = 12.5f,
        )
        client.addPoint(point)
        assertEquals(listOf(point), client.queryPointsBlocking(WorkoutDataQuery.ALL))
    }

    @Test
    fun `window, downsample and range queries encode correctly through the URI`() {
        (1..10).forEach { client.addPoint(WorkoutDataPoint(elapsedTimeSec = it)) }

        assertEquals(
            listOf(6, 7, 8, 9, 10),
            client.queryPointsBlocking(WorkoutDataQuery.lastSeconds(4)).map { it.elapsedTimeSec },
        )
        assertEquals(
            listOf(4, 9, 10),
            client.queryPointsBlocking(WorkoutDataQuery.everySeconds(5)).map { it.elapsedTimeSec },
        )
        assertEquals(
            listOf(3, 4, 5),
            client.queryPointsBetweenBlocking(3, 5).map { it.elapsedTimeSec },
        )
        // open-ended incremental read: everything at or after fromSec
        assertEquals(
            listOf(8, 9, 10),
            client.queryPointsSinceBlocking(8).map { it.elapsedTimeSec },
        )
        assertEquals(
            emptyList<Int>(),
            client.queryPointsSinceBlocking(11).map { it.elapsedTimeSec },
        )
    }

    @Test
    fun `series projection returns the requested metric`() {
        client.addPoint(WorkoutDataPoint(elapsedTimeSec = 1, heartRate = 100, speedKph = 9f))
        client.addPoint(WorkoutDataPoint(elapsedTimeSec = 2, heartRate = 110, speedKph = 10f))

        val hr = client.querySeriesBlocking(WorkoutMetric.HEART_RATE, WorkoutDataQuery.ALL)
        assertEquals(listOf(100f, 110f), hr.map { it.value })
        val speed = client.querySeriesBlocking(WorkoutMetric.SPEED, WorkoutDataQuery.ALL)
        assertEquals(listOf(9f, 10f), speed.map { it.value })
    }

    @Test
    fun `future set-get roundtrip and clear through the client`() {
        val future = WorkoutFutureData(
            listOf(FutureProfile(WorkoutMetric.INCLINE, listOf(FutureProfilePoint(30f, 4.5f))))
        )
        client.setFuture(future)
        assertEquals(future, client.getFutureBlocking())

        client.clearBlocking()
        assertEquals(WorkoutFutureData(), client.getFutureBlocking())
    }

    @Test
    fun `empty store returns empty lists, not errors`() {
        assertEquals(emptyList<WorkoutDataPoint>(), client.queryPointsBlocking(WorkoutDataQuery.ALL))
        assertEquals(WorkoutFutureData(), client.getFutureBlocking())
    }
}
