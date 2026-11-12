package com.jht.sprint8

import com.jht.enums.FrameType
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.sprint8.ui.topbar.Sprint8TopBarManager
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class Sprint8TopBarManagerTest {

    private lateinit var manager: Sprint8TopBarManager

    @MockK
    private lateinit var workoutModel: WorkoutModel

    @MockK
    private lateinit var uiService: UIServiceModel

    @MockK
    private lateinit var uiBaseModel: UIBaseModel

    @MockK
    private lateinit var navigationModel: NavigationModel

    private var showCalled = false
    private var hideCalled = false

    private fun setupVisibilityCallbacks() {
        showCalled = false
        hideCalled = false
        manager.setCallbacks(onShow = { showCalled = true }, onHide = { hideCalled = true })
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        manager = Sprint8TopBarManager(
            workoutModel,
            uiService,
            uiBaseModel,
            navigationModel
        )
    }

    @Test
    fun `updateVisibility shows panel when sprint8 is backgrounded with an active workout`() {
        setupVisibilityCallbacks()
        manager.setIsSprint8Workout(true)
        manager.setIsWorkoutStopped(false)

        manager.updateVisibility(showUi = true)

        assert(showCalled) { "Expected panel to show when backgrounded with active Sprint8 workout" }
        assert(!hideCalled)
    }

    @Test
    fun `updateVisibility hides panel when sprint8 is in the foreground`() {
        setupVisibilityCallbacks()
        manager.setIsSprint8Workout(true)
        manager.setIsWorkoutStopped(false)

        manager.updateVisibility(showUi = false)

        assert(hideCalled) { "Expected panel to hide when Sprint8 is in the foreground" }
        assert(!showCalled)
    }

    @Test
    fun `updateVisibility hides panel when active workout is not sprint8`() {
        setupVisibilityCallbacks()
        manager.setIsSprint8Workout(false)
        manager.setIsWorkoutStopped(false)

        manager.updateVisibility(showUi = true)

        assert(hideCalled) { "Expected panel to hide when active workout is not Sprint8" }
        assert(!showCalled)
    }

    @Test
    fun `Given an Ascent frame type we expect isInclineVisible == true,isResistance == true, isSpeedVisible == false`() {
        manager.setFrameType(FrameType.Ascent)
        manager.updateLayoutBasedOnEquipment()
        val uiState = manager.uiState.value
        assert(uiState.frameType == FrameType.Ascent)
        assert(uiState.isInclineVisible)
        assert(uiState.isResistanceVisible)
        assert(!uiState.isSpeedVisible)
    }

    @Test
    fun `Given a Treadmill frame type we expect isInclineVisible == true,isResistance == false, isSpeedVisible == true`() {
        manager.setFrameType(FrameType.Treadmill)
        manager.updateLayoutBasedOnEquipment()
        val uiState = manager.uiState.value
        assert(uiState.frameType == FrameType.Treadmill)
        assert(uiState.isInclineVisible)
        assert(!uiState.isResistanceVisible)
        assert(uiState.isSpeedVisible)
    }

    @Test
    fun `Given a frame in this list we expect isInclineVisible == false,isResistance == true, isSpeedVisible == false`() {
        val frames = listOf(
            FrameType.Bike, FrameType.SpinBike, FrameType.Climbmill, FrameType.Elliptical,
            FrameType.Stepper
        )

        frames.forEach { frame ->
            manager.setFrameType(frame)
            manager.updateLayoutBasedOnEquipment()
            val uiState = manager.uiState.value
            assert(uiState.frameType == frame)
            assert(!uiState.isInclineVisible)
            assert(uiState.isResistanceVisible)
            assert(!uiState.isSpeedVisible)
        }


    }
}