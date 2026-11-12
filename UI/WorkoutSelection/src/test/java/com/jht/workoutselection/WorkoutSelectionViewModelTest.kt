package com.jht.workoutselection

import com.jht.data.WorkoutItem
import com.jht.enums.WorkoutId
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSelectionViewModelTest {

    @Test
    fun `orderPagerWorkouts reorders SECA to center position`() {
        val workouts = createMockWorkouts(5, 0)
        val result = WorkoutSelectionViewModel.orderPagerWorkouts(workouts)

        assertEquals(WorkoutId.SECA.id, result[2].workoutId)
        assertEquals(2, WorkoutSelectionViewModel.findCenteredIndex(result.size))
    }

    @Test
    fun `orderPagerWorkouts centers SECA when it starts at the end`() {
        val mockWorkouts = createMockWorkouts(5, 4)
        val result = WorkoutSelectionViewModel.orderPagerWorkouts(mockWorkouts)

        assertEquals(WorkoutId.SECA.id, result[2].workoutId)
        assertEquals(2, WorkoutSelectionViewModel.findCenteredIndex(result.size))
    }

    @Test
    fun `orderPagerWorkouts handles small lists`() {
        assertEquals(0, WorkoutSelectionViewModel.orderPagerWorkouts(emptyList()).size)

        val workout = createMockWorkouts(1, 0)
        assertEquals(workout.first(), WorkoutSelectionViewModel.orderPagerWorkouts(workout)[0])
        assertEquals(0, WorkoutSelectionViewModel.findCenteredIndex(workout.size))
    }

    @Test
    fun `orderPagerWorkouts when SECA not present`() {
        val mockWorkouts = createMockWorkouts(2, -1)

        val result = WorkoutSelectionViewModel.orderPagerWorkouts(mockWorkouts)
        assertEquals(mockWorkouts, result)
        assertEquals(0, WorkoutSelectionViewModel.findCenteredIndex(result.size))
    }

    @Test
    fun `orderPagerWorkouts with even number of workouts`() {
        val mockWorkouts = createMockWorkouts(4, 0)

        val result = WorkoutSelectionViewModel.orderPagerWorkouts(mockWorkouts)
        assertEquals(mockWorkouts[0], result[1])
        assertEquals(1, WorkoutSelectionViewModel.findCenteredIndex(result.size))
    }

    @Test
    fun `orderPagerWorkouts does not move SECA if already centered`() {
        val workouts = createMockWorkouts(5, 2) // SECA already at center
        val result = WorkoutSelectionViewModel.orderPagerWorkouts(workouts)

        assertEquals(WorkoutId.SECA.id, result[2].workoutId)
        // Ensure no other workouts shifted unexpectedly
        assertEquals(workouts, result)
    }

    @Test
    fun `orderPagerWorkouts with tiny list of 2`() {
        val workouts = createMockWorkouts(2, 0)
        val result = WorkoutSelectionViewModel.orderPagerWorkouts(workouts)

        assertEquals(WorkoutId.SECA.id, result[0].workoutId)
        assertEquals(0, WorkoutSelectionViewModel.findCenteredIndex(result.size))
    }

    @Test
    fun `orderPagerWorkouts with tiny list of 3`() {
        val workouts = createMockWorkouts(3, 1)
        val result = WorkoutSelectionViewModel.orderPagerWorkouts(workouts)

        assertEquals(WorkoutId.SECA.id, result[1].workoutId)
        assertEquals(1, WorkoutSelectionViewModel.findCenteredIndex(result.size))
    }

    @Test
    fun `orderPagerWorkouts only moves first SECA when multiple present`() {
        val workouts = createMockWorkouts(5, 0).toMutableList()
        // Add a second SECA at the end
        workouts.add(createMockWorkout(WorkoutId.SECA.id))

        val result = WorkoutSelectionViewModel.orderPagerWorkouts(workouts)

        assertEquals(WorkoutId.SECA.id, result[2].workoutId) // first SECA moved to center
        assertEquals(WorkoutId.SECA.id, result.last().workoutId) // second SECA stays at the end
    }

    @Test
    fun `orderPagerWorkouts large list`() {
        val workouts = createMockWorkouts(101, 50) // SECA somewhere in middle
        val result = WorkoutSelectionViewModel.orderPagerWorkouts(workouts)

        val center = WorkoutSelectionViewModel.findCenteredIndex(result.size)
        assertEquals(WorkoutId.SECA.id, result[center].workoutId)
        assertEquals(101, result.size)
    }

    // Helper functions

    private fun createMockWorkout(id: Int): WorkoutItem {
        val item = mockk<WorkoutItem>(relaxed = true)
        every { item.workoutId } returns id
        return item
    }

    private fun createMockWorkouts(totalNum: Int, secaIndex: Int): List<WorkoutItem> {
        val workouts = mutableListOf<WorkoutItem>()
        for (idx in 0..<totalNum) {
            val workout = if (idx == secaIndex) {
                createMockWorkout(WorkoutId.SECA.id)
            } else {
                createMockWorkout(idx)
            }
            workouts.add(workout)
        }

        return workouts
    }
}