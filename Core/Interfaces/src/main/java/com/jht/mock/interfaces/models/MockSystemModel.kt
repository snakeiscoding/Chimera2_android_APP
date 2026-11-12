package com.jht.mock.interfaces.models

import com.jht.data.ISoftwareUpdateStatus
import com.jht.data.TvTunerInformation
import com.jht.data.UserFieldFunction
import com.jht.data.UserFieldText
import com.jht.enums.MinMaxInfo
import com.jht.enums.MinMaxParam
import com.jht.enums.UiEvent
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.BoolVariableListener
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.FloatVariableListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.IntVariableListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.models.SystemModel
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockBoolVariable
import com.jht.mock.interfaces.MockDataValue
import com.jht.mock.interfaces.MockFloatVariable
import com.jht.mock.interfaces.MockIntValue
import com.jht.mock.interfaces.MockIntVariable
import com.jht.mock.interfaces.MockParcelableValue
import com.jht.mock.interfaces.MockStringValue

class MockSystemModel(

    /*Language*/
    override val languageList: List<String> = listOf(""),
    override val currentLanguageIndex: IntValueListener = MockIntValue(0),

    /*Custom Assets*/
    override val logoUri: MockStringValue = MockStringValue(""),
    override val customBackgroundUriList: DataValueListener<List<String>> = MockDataValue(listOf()),
    override val customVideoUriList: DataValueListener<List<String>> = MockDataValue(listOf()),
    override val customBackgroundTimeSeconds: Int = 30,

    /*Language and Date Time*/
    override val errorCode: MockIntValue = MockIntValue(0),
    override val isMachineSetup: BoolValueListener = MockBoolValue(true),
    override val serialNumberFrame: String = "mock_frame_serial_num",
    override val serialNumberConsole: String = "mock_console_serial_num",
    override val serialNumberBridge: String = "mock_bridge_serial_num",
    override val machineUUID: DataValueListener<String> = MockStringValue(""),
    override val facilityID: DataValueListener<Int> = MockIntValue(-1),
    override val accumulatedDistanceKm: Float = -1f,
    override val accumulatedFloors: Float = -1f,
    override val accumulatedTime: Int = -1,
    override val softwareVersion: String = "mock_software_version",
    override val lcbVersion: String = "mock_lcb_version",
    override val lcbType: Int = -1,
    override val wirelessIpAddress: String = "mock_wireless_ip",
    override val wirelessMacAddress: String = "mock_wireless_mac",
    override val wirelessSSID: String = "mock_wireless_ssid",
    override val ethernetIpAddress: String = "mock_ethernet_ip",
    override val ethernetMacAddress: String = "mock_ethernet_mac",
    override val dapiEnvironment: DataValueListener<String> = MockStringValue("mock_dapi_env"),
    override val hasEthernet: Boolean = true,
    override val isInternetAvailable: DataValueListener<Boolean> = MockBoolValue(true),
    override val demoRpm: IntVariableListener = MockIntVariable(0),
    override val demoHeartRate: IntVariableListener = MockIntVariable(0),

    override var screenTimeoutSeconds: Int = -1,
    override var welcomeScreenTimeoutSeconds: Int = -1,
    override val keyGoPressed: MockBoolValue = MockBoolValue(false),
    override val onFrameChange: EventListener = MockEventListener(),
    override val onSplashReset: EventListener = MockEventListener(),
    override val onFactoryReset: EventListener = MockEventListener(),
    override var postWorkoutSurveyUrl: String = "",
    override var mlPostWorkoutSurveyUrl: String = "",
    override var currentLocale: String = "en_US",
    override var appStoreFilterName: String = "",
    override val settingsPasscodeField: UserFieldText? = null,

    ) : SystemModel() {
    /** Events passed to [sendUiEvent], in call order */
    val uiEvents = mutableListOf<UiEvent>()
    /** Number of [enablePhysicalHomeKey] calls */
    var homeKeyEnables = 0
        private set
    /** Number of [reportHomeButtonPress] calls */
    var homeButtonPresses = 0
        private set

    override fun sendUiEvent(uiEvent: UiEvent) {
        uiEvents += uiEvent
    }
    override fun enablePhysicalHomeKey() {
        homeKeyEnables++
    }
    override fun reportHomeButtonPress() {
        homeButtonPresses++
    }

    override fun settingsPasscodeCheck(code: String): Boolean = true
    override fun setLanguage(languageIndex: Int, updateDefault: Boolean) {
        // do nothing
    }

    override fun resetLanguage() {
        TODO("Not yet implemented")
    }

    override val connectWatchResourceId: Int
        get() = 0

    override fun saveLogs() {
        TODO("Not yet implemented")
    }

    override val saveLogsCompleteEvent: EventListener
        get() = MockEventListener()

    override fun processKeypadCode(code: Int): Boolean {
        return false
    }

    override fun rebootSystem(reason: String) {
        TODO("Not yet implemented")
    }

    override fun enterERP() {
        TODO("Not yet implemented")
    }

    override val outOfOrder: BoolVariableListener = MockBoolVariable(false)

    override val audioMute: BoolVariableListener = MockBoolVariable(false)
    override val audioVolume: FloatVariableListener = MockFloatVariable(0.5f)

    override fun setSoftwareUpdateCallback(callback: ISoftwareUpdateStatus) {}
    override val tvTunerInformation: ParcelableValueListener<TvTunerInformation> = MockParcelableValue(TvTunerInformation())
    override val tvTunerConnected: BoolValueListener = MockBoolValue(true)

    override val isWelcomeTheHomeScreen: Boolean
        get() = true

    override fun getMinMaxInfoForParameter(param: MinMaxParam): MinMaxInfo {
        return MinMaxInfo()
    }

    override fun showDebugMenu() {}

    override fun close() {
        // do nothing
    }

    class MockEventListener() :
        EventListener {
        override fun listen(onEvent: () -> Unit) {}
    }
}