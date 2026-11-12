package com.jht.devicepairing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jht.enums.DeviceState
import com.jht.mock.interfaces.models.MockDeviceModel
import com.jht.mock.interfaces.models.MockPairingDevice
import com.jht.mock.interfaces.models.MockPairingHandler
import com.jht.mock.interfaces.models.MockSystemModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockUIServiceModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// just create view model directly? https://stackoverflow.com/a/72680929

@RunWith(AndroidJUnit4::class)
class DevicePairingActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun categoryList() {
        val backend = DevicePairingBackend()
        //backend.deviceModel.mockPairingMediaDevice = null
        backend.deviceModel.mockPairingAudioOut = MockPairingHandler()
        backend.deviceModel.mockPairingHeartRate = MockPairingHandler()
        backend.deviceModel.mockPairingFtms = null

        rule.setContent {
            DevicePairingRoute(devicePairingViewModel = backend.viewModel)
        }
        backend.onResume()
        // Make sure only the supported categories are displayed
        //rule.onNodeWithTag(TestTagMedia).assertIsNotDisplayed()
        rule.onNodeWithTag(TestTagAudioOut).assertIsDisplayed()
        rule.onNodeWithTag(TestTagHearRate).assertIsDisplayed()
        rule.onNodeWithTag(TestTagFtms).assertIsNotDisplayed()
        backend.onPause()
    }

    @Test
    fun deviceList() {
        val backend = DevicePairingBackend()
        val device1 = MockPairingDevice("Device1", "Device:1")
        val tagDevice1 = tagDevice(device1.deviceId)
        val tagDevice1Disconnect = "${tagDevice1}:Disconnect" // creative wants to try limiting disconnect to touching icon
        backend.deviceModel.mockPairingHeartRate = MockPairingHandler()
        backend.deviceModel.mockPairingAudioOut = MockPairingHandler()

        rule.setContent {
            DevicePairingRoute(devicePairingViewModel = backend.viewModel)
        }
        backend.onResume()
        backend.deviceModel.apply {
            mockPairingHeartRate?.addDevice(device1)
        }

        // go to HR, make sure device is listed as disconnected
        rule.onNodeWithTag(TestTagHearRate).performClick()
        rule.onNodeWithTag(tagDevice1).assertIsDisplayed()
        rule.onNodeWithTag(tagNoDeviceConnected).assertIsDisplayed()
        rule.onNodeWithTag(tagNoDeviceDisconnected).assertIsNotDisplayed()

        // connect device, make sure if shifts lists
        rule.onNodeWithTag(tagDevice1).performClick()
        assertEquals(device1.connectionState, DeviceState.Connecting)
        backend.deviceModel.mockPairingHeartRate?.changeState(device1, DeviceState.Connected)
        rule.onNodeWithTag(tagDevice1).assertIsDisplayed()
        rule.onNodeWithTag(tagNoDeviceConnected).assertIsNotDisplayed()
        rule.onNodeWithTag(tagNoDeviceDisconnected).assertIsDisplayed()

        // disconnect device, make sure if shifts lists
        rule.onNodeWithTag(tagDevice1Disconnect).assertIsDisplayed()
        rule.onNodeWithTag(tagDevice1Disconnect).performClick()
        assertEquals(device1.connectionState, DeviceState.Disconnected)
        rule.onNodeWithTag(tagDevice1).assertIsDisplayed()
        rule.onNodeWithTag(tagNoDeviceConnected).assertIsDisplayed()
        rule.onNodeWithTag(tagNoDeviceDisconnected).assertIsNotDisplayed()

        // Go to empty category, make sure there are no devices
        rule.onNodeWithTag(TestTagAudioOut).performClick()
        rule.onNodeWithTag(tagDevice1).assertIsNotDisplayed()
        rule.onNodeWithTag(tagNoDeviceConnected).assertIsDisplayed()
        rule.onNodeWithTag(tagNoDeviceDisconnected).assertIsDisplayed()

        backend.onPause()
    }

} // END CLASS: DevicePairingActivityTest


private class DevicePairingBackend {
    val systemModel = MockSystemModel()
    val uiBaseModel = MockUIBaseModel()
    val deviceModel = MockDeviceModel()
    val uiServiceModel = MockUIServiceModel()
    val workoutModel = MockWorkoutModel()
    val viewModel = DevicePairingViewModel(systemModel, uiBaseModel, uiServiceModel, deviceModel, workoutModel)
    fun onResume() { viewModel.start() }
    fun onPause() { viewModel.stop() }
}

private fun tagDevice(id: String) = "Device:$id"
private const val tagNoDeviceConnected = "NoDevices:Connected"
private const val tagNoDeviceDisconnected = "NoDevices:Disconnected"
