package com.jht.firsttimesetup

import com.jht.firsttimesetup.viewmodels.EditMachineInfoViewModel
import com.jht.mock.interfaces.models.MockSystemModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import junit.framework.TestCase.assertEquals
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnitParamsRunner::class)
class EditMachineInfoViewModelTest {

    private lateinit var vm: EditMachineInfoViewModel

    private val systemModel = MockSystemModel()

    private val uiBaseModel = MockUIBaseModel()

    private val ftsuModel = MockFirstTimeSetupModel()

    @Before
    fun setup() {
        vm = EditMachineInfoViewModel(
            systemModel,
            uiBaseModel,
            ftsuModel,
        )
    }


    @Test
    fun autoFillTest() {
        val newFramePrefix = "IOP789"
        val expectedNewSn = "${newFramePrefix}mock_frame_serial_num"
        val oldPrefixList = listOf("XCV234", "GHJ567", "ABC123", "YUI890")

        vm.machineInfo.value

        vm.autofillPrefix(newFramePrefix, oldPrefixList)

        assertEquals(expectedNewSn, vm.machineInfo.value.frameSn)
    }
}