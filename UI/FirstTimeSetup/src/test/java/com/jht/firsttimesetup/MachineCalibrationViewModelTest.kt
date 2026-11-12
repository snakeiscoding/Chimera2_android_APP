package com.jht.firsttimesetup

import com.jht.firsttimesetup.viewmodels.MachineCalibrationState
import com.jht.firsttimesetup.viewmodels.MachineCalibrationViewModel
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class MachineCalibrationViewModelTest {

    private lateinit var vm: MachineCalibrationViewModel

    @MockK
    private lateinit var ftsuModel: FirstTimeSetupModel


    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        vm = MachineCalibrationViewModel(
            ftsuModel
        )
    }

    @Test
    fun `Given startCalibration called we expect ftsu model to calibrate`() {
        vm.startCalibration()
        verify(exactly = 1) { ftsuModel.startCalibration() }
    }

    @Test
    fun `Given triggerMachineCalibration called we expect ui state to be CALIBRATING`() {
        vm.triggerMachineCalibration()
        val uiState = vm.uiState
        assertEquals(MachineCalibrationState.CALIBRATING, uiState.value.state)
    }

    @Test
    fun `Given triggerMachineCalibrationSuccess called we expect ui state to be SUCCESS`() {
        vm.triggerMachineCalibrationSuccess()
        val uiState = vm.uiState
        assertEquals(MachineCalibrationState.SUCCESS, uiState.value.state)
    }

    @Test
    fun `Given triggerMachineCalibrationFailure called we expect ui state to be FAIL`() {
        vm.triggerMachineCalibrationFailure()
        val uiState = vm.uiState
        assertEquals(MachineCalibrationState.FAIL, uiState.value.state)
    }

    @Test
    fun `Given onRestartDemo called we expect ui state to be START`() {
        vm.onRestartDemo()
        val uiState = vm.uiState
        assertEquals(MachineCalibrationState.START, uiState.value.state)
    }
}