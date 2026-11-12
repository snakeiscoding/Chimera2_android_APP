package com.jht.welcome

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jht.data.QrAuthorizationResponse
import com.jht.data.LoginData
import com.jht.data.LoginType
import com.jht.data.MarketingAd
import com.jht.data.QrLoginHandler
import com.jht.data.RegisterData
import com.jht.data.RfidLoginData
import com.jht.data.RfidOperatingMode
import com.jht.data.UserField
import com.jht.data.UserFieldFunction
import com.jht.enums.RelativeLocation
import com.jht.enums.UiEvent
import com.jht.enums.WifiState
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.LaunchWelcomeScreenOptions
import com.jht.interfaces.models.LoginModel
import com.jht.interfaces.models.MarketingModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import com.jht.widgets.RfidLoginWidgetHandler
import com.jht.widgets.helpers.AsyncUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.TimeSource

private const val TAG = "WelcomeViewModel"

@HiltViewModel
open class WelcomeViewModel @Inject constructor(
    val systemModel: SystemModel,
    val uiBaseModel: UIBaseModel,
    val idleTimerModel: IdleTimerModel,
    val loginModel: LoginModel,
    val workoutLauncherModel: WorkoutLauncherModel,
    val navigationModel: NavigationModel,
    val deviceModel: DeviceModel,
    val marketingModel: MarketingModel,
    val savedStateHandle: SavedStateHandle, // can provide launch intent extras
) : ThemedViewModel() {
    // variables for timing ptp login
    private val timeSource = TimeSource.Monotonic
    private var startTimingLogin = timeSource.markNow()
    private var endTimingLogin = timeSource.markNow()

    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    val backgroundUri by lazy { mutableStateOf<Uri>(Uri.EMPTY) }
    val advertisingList = mutableStateOf(listOf<MarketingAd>())
    val message = mutableStateOf("") // TODO AM4 message (message center)
    val uiState = mutableStateOf(AsyncUiState.Unknown)
    val loginList = mutableStateOf(listOf<LoginData>())
    val registerList = mutableStateOf(listOf<RegisterData>())
    val selectedTabKey = mutableStateOf<Any?>(null) // trying to hoist state to here without using UI elements in the view model, key seemed to work?
    val currentLanguageIndex = mutableStateOf(0)
    val screenTimedOut = mutableStateOf(false)
    val customVideoList = mutableStateOf(listOf<Uri>())
    val networkAvailable = mutableStateOf(false)
    var qrUiState by mutableStateOf(
        QrUiState(
            onShowQrClicked = ::onShowQrClicked,
            onQrDismissed = ::stopQrSession,
        )
    )
        private set

    private val homeScreenIdleTimer = idleTimerModel.createHomeScreenSystemTimer(systemModel.isWelcomeTheHomeScreen)
    private var screenBackgroundTimer: Job? = null
    private var customBackgroundList = listOf<Uri>()

    /**
     * Call this to register listeners when activity is resumed
     */
    override fun start() {
        systemModel.sendUiEvent(UiEvent.WELCOME_SCREEN)
        deviceModel.wifiState.listenAndTrigger { networkAvailable.value = (it != WifiState.NO_CONNECTION) }
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
        systemModel.customBackgroundUriList.listenAndTrigger(::refreshBackgroundSlideList)
        systemModel.customVideoUriList.listenAndTrigger(::refreshVideoSlideList)
        marketingModel.welcomeAds.listenAndTrigger {
            advertisingList.value = it.list
        }
        systemModel.currentLanguageIndex.listenAndTrigger {
            currentLanguageIndex.value = it
            loginList.value = loginModel.getLoginOptions()
            registerList.value = loginModel.getRegisterOptions()
        }
        idleTimerModel.addIdleTimer("HomeOverlay", systemModel.welcomeScreenTimeoutSeconds * 1000) {
            refreshAdvert()
            setTab(null)
            screenTimedOut.value = true
            stopQrSession()
            systemModel.sendUiEvent(UiEvent.SCREENSAVER_SCREEN)
        }
        idleTimerModel.startIdleTimers(wakeCallback = {
            screenTimedOut.value = false
            systemModel.sendUiEvent(UiEvent.WELCOME_SCREEN)
        })
        homeScreenIdleTimer.start()
        nextRfidScan()
        startScreenBackgroundTimer()
        applyLaunchOptions()
        systemModel.keyGoPressed.listen {
            if (it && uiState.value != AsyncUiState.Waiting) {
                workoutLauncherModel.checkLaunchWorkoutPin { proceed ->
                    if (proceed) workoutLauncherModel.launchQuickStartWorkout()
                }
            }
        }
    }

    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        setTab(null) // always force deselect to ensure dialog side effects like wallet mode are cleared
        stopQrSession()
        deviceModel.close()
        idleTimerModel.close()
        homeScreenIdleTimer.stop()
        systemModel.close()
        loginModel.close()
        marketingModel.close()
    }

    /** dismiss all the ads or a specific one if given */
    fun dismissAdvert(itemToDismiss: MarketingAd? = null) {
        if (itemToDismiss == null) advertisingList.value = emptyList()
        else advertisingList.value = advertisingList.value - itemToDismiss
    }
    /** re-populate the ad list */
    private fun refreshAdvert() {
        advertisingList.value = marketingModel.welcomeAds.value.list
    }

    private val qrHandler = object : QrLoginHandler {
        override fun onCodeReady(response: QrAuthorizationResponse) {
            qrUiState = qrUiState.copy(
                qrUri = response.verificationUriComplete,
                userCode = response.userCode,
                verificationUri = response.verificationUri,
            )
        }

        override fun onExpired() {
            qrUiState = qrUiState.copy(qrUri = null, userCode = null, verificationUri = null, qrShown = false)
            uiState.value = AsyncUiState.Unknown
        }

        override fun onError() {
            qrUiState = qrUiState.copy(qrUri = null, userCode = null, verificationUri = null, qrShown = false)
            uiState.value = AsyncUiState.Failure
        }

        override fun onLoginSuccess() {
            qrUiState = qrUiState.copy(qrUri = null, userCode = null, verificationUri = null, qrShown = false)
            navigationModel.launchGoScreen()
        }
    }

    fun onShowQrClicked() {
        if (screenTimedOut.value) return
        if (loginList.value.none { it.type == LoginType.QrCode }) return
        // Always tear down any prior session and start fresh
        loginModel.qrStopSession()
        qrUiState = qrUiState.copy(qrUri = null, userCode = null, verificationUri = null, qrShown = true)
        uiState.value = AsyncUiState.Waiting
        loginModel.qrStartSession(qrHandler)
    }

    fun stopQrSession() {
        loginModel.qrStopSession()
        qrUiState = qrUiState.copy(qrUri = null, userCode = null, verificationUri = null, qrShown = false)
        if (uiState.value == AsyncUiState.Waiting) {
            uiState.value = AsyncUiState.Unknown
        }
    }

    fun resetIdle() {
        // typically it is not necessary to manually reset idle as the system is resetting on hardware events
        // however it does not presently catch software events and we do not want to get stuck on screen saver
        idleTimerModel.resetIdleTimers()
    }

    private fun resetScreen() {
        uiState.value = AsyncUiState.Unknown
        loginList.value.forEach { it.clearValues() }
        registerList.value.forEach { it.clearValues() }
    }

    /** called by UI to select or deselect tab, can be blocked under certain conditions */
    fun onSelectTabKey(key: Any? = null) {
        if (key == null || uiState.value != AsyncUiState.Waiting || screenTimedOut.value) {
            setTab(key)
        }
    }
    private fun setTab(key: Any? = null) {
        // is it worth creating a TabKey object that holds a key:Any? but we use the container class to maintain strong types?
        val qrKey = makeLoginKey(LoginType.QrCode)
        val leavingQr = selectedTabKey.value?.toString() == qrKey && key?.toString() != qrKey
        selectedTabKey.value = key
        resetScreen()
        // wallet login needs special RFID mode
        val walletKey = makeLoginKey(LoginType.Wallet)
        val rfidMode = if (key?.toString()?.contains(walletKey) == true) RfidOperatingMode.Wallet else RfidOperatingMode.Normal
        setRfidMode(rfidMode)
        if (leavingQr) stopQrSession()
        // just dismiss all ads when user leave default start panel
        if (key != null) dismissAdvert()
    }

    private fun applyLaunchOptions() {
        savedStateHandle.get<LaunchWelcomeScreenOptions>(LaunchWelcomeScreenOptions.key)?.let { options->
            options.preselectLogin?.let { loginType ->
                loginList.value.find { it.type == loginType }?.let { login->
                    setTab(makeLoginKey(login))
                    options.associateDevice?.let { device ->
                        login.associateDeviceLogin = device
                    }
                }
            }
        }
    }

    fun attemptLogin(data: LoginData) {
        if (uiBaseModel.isDebugBuild) startTimingLogin = timeSource.markNow()

        //We only need to indicate waiting for non-guest logins
        if (!data.isGuest()){
            uiState.value = AsyncUiState.Waiting
        }

        // if loginModel is given a NULL data, it will assume guest
        loginModel.attemptLogin(data) {
            if (it == 0) {
                if (uiBaseModel.isDebugBuild) {
                    // there is still timing available in AnalyticsManager that tracks the datamatrix keys
                    // associated with login, but I like the simplicity of this kotlin timing (auto formatted with unit)
                    endTimingLogin = timeSource.markNow()
                    Log.i(TAG, "ptpn attemptLogin timing: ${endTimingLogin - startTimingLogin}")
                }
                //success
                navigationModel.launchGoScreen()
            } else {
                //failure
                uiState.value = AsyncUiState.Failure
            }
        }
    }

    fun attemptRegister(data: RegisterData) {
        uiState.value = AsyncUiState.Waiting
        loginModel.attemptRegister(data) {
            if (it == 0) {
                // success
                setTab()
                // registration should have logged new user into console (if system has option not to, maybe we pass a parameter to attemptRegister to specify?)
                navigationModel.launchGoScreen()
            } else {
                //failure
                uiState.value = AsyncUiState.Failure
            }
        }
    }

    /** Starts the timer for cycling backgrounds. It will update the current background URI. */
    private fun startScreenBackgroundTimer() {
        if (screenBackgroundTimer == null) { // only create one of these
            screenBackgroundTimer = viewModelScope.launch {
                var index = 0
                customBackgroundFlow().collect {
                    if (customBackgroundList.isNotEmpty()) {
                        index %= customBackgroundList.size
                        backgroundUri.value = customBackgroundList[index++]
                    }
                }
            }
        }
    }
    /** Kotlin flow timer for the custom backgrounds. Essentially a periodic timer */
    private fun CoroutineScope.customBackgroundFlow() = flow {
        //Initializing delay time so multiple get() are not needed on subsequent loops
        val delayTime = systemModel.customBackgroundTimeSeconds * 1000L
        while (isActive) {
            emit(Unit)
            delay(delayTime)
        }
    }
    /** called when there is a problem with the background media */
    fun onMediaError(e: Exception) {
        Log.w(TAG, "Media playback issue", e)
        // manually trigger a list refresh? or should we just clear the lists until next time?
        // system side could use an android.os.FileObserver to trigger list updates, then our listeners would just fire as needed?
        // at the moment the biggest issue is videos being removed in background, so maybe just clear out videos until next pass?
        // it might also be possible that bad files could trigger this as well, so do we need to avoid an error loop?
        //refreshBackgroundSlideList()
        //refreshVideoSlideList()
        customVideoList.value = listOf()
    }
    /** refresh the list of background images for slide show */
    private fun refreshBackgroundSlideList(list: List<String> = systemModel.customBackgroundUriList.value) {
        customBackgroundList = list.map {uriString-> Uri.parse(uriString) }
        //Initialize background
        if (customBackgroundList.isNotEmpty()) {
            backgroundUri.value = customBackgroundList[0]
        }
    }
    /** refresh the list of background videos for slide show */
    private fun refreshVideoSlideList(list: List<String> = systemModel.customVideoUriList.value) {
        customVideoList.value = list.map {uriString-> Uri.parse(uriString) }
    }

    val rfidHandler = RfidLoginWidgetHandler(
        inProgress = { inProgress-> uiState.value = if (inProgress) AsyncUiState.Waiting else AsyncUiState.Unknown },
        loginComplete = { navigationModel.launchGoScreen() },
        needToAssociate = ::gotoXidLoginToAssociate,
        readyForNextScan = ::nextRfidScan,
    )
    private fun nextRfidScan() {
        loginModel.rfidStartSession(rfidHandler)
    }
    private fun gotoXidLoginToAssociate(rfid: RfidLoginData) {
        val xidLogin = loginList.value.find { it.type == LoginType.Xid }
        if (xidLogin != null) {
            xidLogin.associateDeviceLogin = rfid
            selectedTabKey.value = makeLoginKey(xidLogin)
        }
    }
    fun setRfidMode(mode: RfidOperatingMode) {
        loginModel.rfidSetMode(mode)
    }

    companion object {
        fun makeLoginKey(login: LoginData) = makeLoginKey(login.type)
        fun makeLoginKey(loginType: LoginType) = "login:${loginType.value}"
        fun makeRegisterKey(register: RegisterData) = makeRegisterKey(register.type)
        fun makeRegisterKey(loginType: LoginType) = "register:${loginType.value}"
    }

    fun showDebugDialog() {
        if (BuildConfig.DEBUG) {
            systemModel.showDebugMenu()
        }
    }
    internal fun needsRfidIcon(): Boolean {
        return deviceModel.rfidReaderLocation == RelativeLocation.LEFT_MIDDLE
    }
}


internal fun sortFilterRegisterOptions(rawList: List<RegisterData>, fieldOrder: List<UserFieldFunction>): List<RegisterData> {
    val newList = mutableListOf<RegisterData>()
    rawList.forEach { data ->
        newList.add(sortFilterRegisterData(data, fieldOrder))
    }
    return newList
}
internal fun sortFilterRegisterData(rawData: RegisterData, fieldOrder: List<UserFieldFunction>): RegisterData {
    return rawData.copy(fields = sortFilterUserField(rawData.fields, fieldOrder))
}
internal fun sortFilterUserField(rawList: List<UserField>, fieldOrder: List<UserFieldFunction>) : List<UserField> {
    val newList = mutableListOf<UserField>()
    fieldOrder.forEach { function ->
        rawList.find { it.function == function }?.let {
            newList.add(it)
        }
    }
    return newList
}
