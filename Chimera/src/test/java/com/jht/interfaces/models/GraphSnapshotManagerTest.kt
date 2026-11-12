package com.jht.interfaces.models

import com.jht.workoutdata.model.WorkoutDataPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Logic tests for the sliding-window snapshot state machine, against a fake source.
 * Window sizes are injected so the tests don't depend on the global WorkoutGraphConfig.
 */
class GraphSnapshotManagerTest {

    /** Fake provider store: seconds present in the store, plus call counting for probe checks. */
    private class FakeSource : GraphSnapshotManager.Source {
        val store = sortedMapOf<Int, WorkoutDataPoint>()
        val sinceCalls = mutableListOf<Int>()
        val betweenCalls = mutableListOf<Pair<Int, Int>>()
        private var generation = 1L

        fun add(vararg secs: Int) = secs.forEach { store[it] = WorkoutDataPoint(elapsedTimeSec = it) }

        /** Models the provider's clear + repopulate: wipes the store and bumps the generation. */
        fun reset(vararg secs: Int) {
            store.clear()
            generation++
            add(*secs)
        }

        override fun window(minutes: Int): List<WorkoutDataPoint> {
            val newest = store.keys.lastOrNull() ?: return emptyList()
            val since = newest - minutes * 60
            return store.filterKeys { it >= since }.values.toList()
        }

        override fun since(fromSec: Int): List<WorkoutDataPoint> {
            sinceCalls.add(fromSec)
            return store.filterKeys { it >= fromSec }.values.toList()
        }

        override fun between(fromSec: Int, toSec: Int): List<WorkoutDataPoint> {
            betweenCalls.add(fromSec to toSec)
            return store.filterKeys { it in fromSec..toSec }.values.toList()
        }

        override fun generation(): Long = generation
    }

    private fun manager(source: FakeSource, windowMinutes: Int = 2) =
        GraphSnapshotManager(source, { windowMinutes }, { windowMinutes * 60 })

    private fun GraphSnapshotManager.seconds() = snapshot.map { it.elapsedTimeSec }

    @Test
    fun `first refresh bulk loads the window`() {
        val source = FakeSource().apply { add(1, 2, 3) }
        val manager = manager(source)
        manager.refresh()
        assertEquals(listOf(1, 2, 3), manager.seconds())
    }

    @Test
    fun `subsequent refresh appends only points newer than the last seen`() {
        val source = FakeSource().apply { add(1, 2, 3) }
        val manager = manager(source)
        manager.refresh()
        source.sinceCalls.clear()

        source.add(4, 5)
        manager.refresh()

        assertEquals(listOf(1, 2, 3, 4, 5), manager.seconds())
        // incremental fetch asked only for everything since lastSeen+1
        assertEquals(listOf(4), source.sinceCalls)
    }

    @Test
    fun `single-second gap is backfilled when the data appears later`() {
        val source = FakeSource().apply { add(1, 2, 4) } // 3 missing at bulk load
        val manager = manager(source)
        manager.refresh()
        assertEquals(listOf(1, 2, 4), manager.seconds())

        source.add(3, 5) // 3 arrives late, 5 is new
        manager.refresh()
        assertEquals(listOf(1, 2, 3, 4, 5), manager.seconds())
    }

    @Test
    fun `genuinely empty seconds are probed once and never re-requested`() {
        val source = FakeSource().apply { add(1, 2, 4) } // 3 never exists
        val manager = manager(source)
        manager.refresh()

        source.add(5)
        manager.refresh() // probes 3, finds nothing -> cached
        source.add(6)
        manager.refresh() // must NOT probe 3 again

        val probesForThree = source.betweenCalls.count { it == (3 to 3) }
        assertEquals(1, probesForThree)
        assertEquals(listOf(1, 2, 4, 5, 6), manager.seconds())
    }

    @Test
    fun `reset (new generation, elapsed restarted) triggers a bulk reload`() {
        val source = FakeSource().apply { add(100, 101, 102) }
        val manager = manager(source)
        manager.refresh()
        assertEquals(listOf(100, 101, 102), manager.seconds())

        source.reset(1, 2) // new workout started; store cleared and restarted
        manager.refresh()
        assertEquals(listOf(1, 2), manager.seconds())
    }

    @Test
    fun `reset with the new workout already past the old elapsed time is not spliced`() {
        // Refreshes stall (e.g. the graph screen is not visible), the workout is cleared,
        // and a new one runs PAST the old lastAppendedSec before the next refresh. since()
        // would return contiguous-looking points, so only the generation change can reveal
        // that the cached history belongs to the previous workout.
        val source = FakeSource().apply { add(98, 99, 100) }
        val manager = manager(source)
        manager.refresh()
        assertEquals(listOf(98, 99, 100), manager.seconds())

        source.reset(*(1..110).toList().toIntArray()) // new workout, clock already at 110
        manager.refresh()

        // The whole snapshot is the new workout's window; nothing from the old one survives.
        assertEquals((1..110).toList(), manager.seconds())
    }

    @Test
    fun `empty store after reset yields empty snapshot`() {
        val source = FakeSource().apply { add(10, 11) }
        val manager = manager(source)
        manager.refresh()

        source.reset() // provider cleared, nothing new yet
        manager.refresh()
        assertEquals(emptyList<Int>(), manager.seconds())
    }

    @Test
    fun `snapshot is evicted to the sliding window`() {
        val windowMinutes = 1 // 60 seconds
        val source = FakeSource()
        val manager = manager(source, windowMinutes)
        source.add(1)
        manager.refresh()

        // stream 300 seconds of workout, refreshing as we go
        for (sec in 2..300) {
            source.add(sec)
            manager.refresh()
        }

        val seconds = manager.seconds()
        assertEquals(300, seconds.last())
        // window: newest - 60 = 240 -> everything >= 240 kept, size <= windowSeconds
        assertEquals(240, seconds.first())
        assertEquals(61, seconds.size)
    }

    @Test
    fun `already-seen seconds are not re-fetched - append-only semantics`() {
        val source = FakeSource().apply { add(1, 2) }
        val manager = manager(source)
        manager.refresh()

        // the same second is later rewritten in the store (e.g. duplicate poll tick):
        // the snapshot keeps its original value because appends only fetch newer seconds
        source.store[2] = WorkoutDataPoint(elapsedTimeSec = 2, heartRate = 99)
        source.add(3)
        manager.refresh()

        assertEquals(listOf(1, 2, 3), manager.seconds())
        assertEquals(0, manager.snapshot.first { it.elapsedTimeSec == 2 }.heartRate)
    }

    @Test
    fun `bulk load de-duplicates multiple rows for the same second keeping the latest`() {
        val source = object : GraphSnapshotManager.Source {
            override fun window(minutes: Int) = listOf(
                WorkoutDataPoint(elapsedTimeSec = 1, heartRate = 80),
                WorkoutDataPoint(elapsedTimeSec = 1, heartRate = 85), // duplicate second
                WorkoutDataPoint(elapsedTimeSec = 2, heartRate = 90),
            )
            override fun since(fromSec: Int) = emptyList<WorkoutDataPoint>()
            override fun between(fromSec: Int, toSec: Int) = emptyList<WorkoutDataPoint>()
            override fun generation(): Long = 1L
        }
        val manager = GraphSnapshotManager(source, { 2 }, { 120 })
        manager.refresh()

        assertEquals(listOf(1, 2), manager.snapshot.map { it.elapsedTimeSec })
        assertEquals(85, manager.snapshot.first().heartRate) // last row for the second wins
    }

    @Test
    fun `no new data and no reset keeps the snapshot stable`() {
        val source = FakeSource().apply { add(1, 2, 3) }
        val manager = manager(source)
        manager.refresh()
        manager.refresh() // nothing new, newest unchanged -> not a reset
        assertEquals(listOf(1, 2, 3), manager.seconds())
    }
}
