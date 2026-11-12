package com.jht.streamingapps

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import com.jht.data.AppInfo
import com.jht.interfaces.models.RemoteKeys
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.VideoInput
import com.jht.interfaces.models.VideoInputModel
import com.jht.interfaces.ui.MessageCenterMessage
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

private const val TAG = "StreamingAppsViewModel"
internal const val lgScrollThreshold: Float = 100f
internal const val lgSwipeThreshold: Float = 80f

@HiltViewModel
open class StreamingAppsViewModel @Inject constructor(
    private val systemModel: SystemModel,
    private val uiBaseModel: UIBaseModel,
    private val uiServiceModel: UIServiceModel,
    private val videoInputModel: VideoInputModel,
    private val application: Application, // app context? do we really need it?
    savedStateHandle: SavedStateHandle, // can provide launch intent extras
) : ThemedViewModel() {
    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    val appList = mutableStateOf(listOf<AppInfo>())
    val videoInput = mutableStateOf(VideoInput())
    val connectionUrl = mutableStateOf("")
    val showOverlay = mutableStateOf(true) // start with overlay to give user instruction
    val noSignal = mutableStateOf(false) // assume signal is ok unless we know that it is not
    val safeRegionPx = mutableStateOf(android.graphics.Rect()) // interface gives old rect in px, composable can convert to new rect Dp
    //val noSignal = mutableStateOf(false)
    private val distanceTracker = DistanceTracker(lgScrollThreshold)
    // can we re-use "url" from app info, or do we need to create a more generic means of key+value passing?
    private var preselectedApp = savedStateHandle.get<Bundle>("extras")?.getString("url")
    private var lastApp = ""
    private var lastLocale = ""
    private var needsInstructions = true

    fun onNewIntent(intent: Intent?) {
        // if activity is relaunched with different preselect app, it will trigger new intent, so update before resume
        val newApp = intent?.extras?.getBundle("extras")?.getString("url")
        Log.d(TAG, "onNewIntent newApp=$newApp, oldApp=$preselectedApp")
        preselectedApp = newApp
    }

    override fun start() {
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
        videoInput.value = videoInputModel.applyVideoInput()
        appList.value = videoInputModel.externalAppList
        // if there is no QR code, set it up
        if (connectionUrl.value.isEmpty()) {
            updateSoftAp()
            lastApp = "" // apps should re-launch if softAP changes
        }
        // make sure an app is under the menu?
        val passedApp = preselectedApp
        Log.d(TAG, "passedApp=$passedApp")
        // if an app was passed at launch, make sure it is up; otherwise if there is no previous app, pick the first available
        //if (passedApp != null && lastApp != passedApp) {
        if (passedApp != null) { // it's ok to ask for relaunch
            Log.d(TAG, "Launching at start $passedApp, previous=$lastApp")
            lastApp = passedApp
            videoInputModel.launchExternalApp(lastApp)
            showOverlay.value = true
        } else if (lastApp.isEmpty() && appList.value.isNotEmpty()) {
            lastApp = appList.value.first().uuid
            Log.d(TAG, "Launching at start $lastApp")
            videoInputModel.launchExternalApp(lastApp)
        }
        // do we need to check for HDMI disconnect specifically or just rely on the widget's error reporting?
        videoInputModel.setInputChangeListener { info ->
            val valid = info.isValid()
            Log.d(TAG, "Video input ${info.widthPixels}x${info.heightPixels} valid=$valid")
            noSignal.value = !valid
        }
        uiServiceModel.overlayViewportPixels.listenAndTrigger {
            safeRegionPx.value = it
        }
        if (needsInstructions) {
            Log.d(TAG, "show instructions")
            // could move this into compose as an effect instead of getting string resource here?
            uiServiceModel.sendMessageCenterMessage(MessageCenterMessage(
                messageHeader = "",
                messageText = application.getString(R.string.streaming_app_instructions_pg),
                priority = 30,
                hideTime = 10,
                tag = TAG,
            ))
            needsInstructions = false
        }
    }

    override fun stop() {
        uiServiceModel.removeMessageCenterMessageByTag(TAG)
        systemModel.close()
        uiBaseModel.close()
        uiServiceModel.close()
        videoInputModel.close()
    }

    private fun updateSoftAp() {
        val ssid = systemModel.serialNumberConsole.take(21)
        val pw = UUID.randomUUID().toString().take(64) // UUID string should only be 36 characters long and URL safe
        if (ssid.isEmpty()) {
            Log.e(TAG, "Cannot set soft AP to empty SSID")
            connectionUrl.value = ""
        } else {
            videoInputModel.setSoftAp(ssid, pw)
            connectionUrl.value = "WIFI:T:WPA;S:$ssid;P:$pw;H:true;;"
            Log.d(TAG, "QR code set ${connectionUrl.value}")
        }
    }

    fun onLocaleChange(code: String) {
        if (lastLocale != code) {
            lastLocale = code
            Log.d(TAG, "onLocaleChange $code")
            videoInputModel.setLocale(code)
        }
    }

    fun onShowOverlay() {
        showOverlay.value = true
    }
    fun onCloseOverlay() {
        showOverlay.value = false
    }

    fun onBack() {
        videoInputModel.sendKey(RemoteKeys.Back)
    }

    fun onTouchDown(x: Float, y: Float) {
        // pass relative 0-1f values to model which will apply to given STB coordinates
        Log.d(TAG, "onTouchDown scaled $x $y")
        videoInputModel.moveMouseTo(x, y)
        distanceTracker.reset()
    }

    fun onTouchTapUp() {
        videoInputModel.clickMouse()
    }

    //#region One single swipe action to one press CGC-22240
    fun onDrag(x: Float, y: Float) {
        Log.d(TAG, "onDrag $x, $y")
        val directionH = if (x < 0) RemoteKeys.DirectionRight else RemoteKeys.DirectionLeft
        val directionV = if (y < 0) RemoteKeys.DirectionDown else RemoteKeys.DirectionUp
        if (abs(x) > lgSwipeThreshold) {
            Log.d(TAG, "send remote key $directionH")
            videoInputModel.sendKey(directionH, 1)
        }
        if (abs(y) > lgSwipeThreshold) {
            Log.d(TAG, "send remote key $directionV")
            videoInputModel.sendKey(directionV, 1)
        }
    }
    //#endregion One swipe to one press

    //#region SCROLL distance to one or more remote presses while it happens
    fun onTouchScroll(distanceX: Float, distanceY: Float) {
        if (distanceTracker.add(distanceX, distanceY)) {
            Log.d(TAG, "onScroll threshold " + distanceTracker.x + "," + distanceTracker.y)
            convertTouchMotionToKeyPress(distanceTracker.x, distanceTracker.y)
            distanceTracker.reset()
        }
    }
    private fun convertTouchMotionToKeyPress(dx: Float, dy: Float) {
        // should we do multiple presses depending on distance or just keep to one press per threshold trigger?
        if (abs(dx.toDouble()) > abs(dy.toDouble())) { // horizontal
            val presses = convertSwipeToPress(dx)
            if (dx < 0) {
                Log.d(TAG, "left $presses") // drag left
                videoInputModel.sendKey(RemoteKeys.DirectionRight, presses)
            } else {
                Log.d(TAG, "right $presses") // drag right
                videoInputModel.sendKey(RemoteKeys.DirectionLeft, presses)
            }
        } else { // vertical
            val presses = convertSwipeToPress(dy)
            if (dy < 0) {
                Log.d(TAG, "up $presses") // drag up
                videoInputModel.sendKey(RemoteKeys.DirectionDown, presses)
            } else {
                Log.d(TAG, "down $presses") // drag down
                videoInputModel.sendKey(RemoteKeys.DirectionUp, presses)
            }
        }
    }
    private fun convertSwipeToPress(delta :Float): Int {
        //return abs((delta / lgScrollThreshold).roundToInt()) // do button press per threshold
        return 1 // just do one press once past threshold
    }
    //#endregion SCROLL to multiple presses
}


/** Tracks accumulated distance and indicates when a threshold has been crossed */
private class DistanceTracker(
    val threshold: Float = 10f,
) {
    var x = 0f
    var y = 0f

    fun reset() { x = 0f; y = 0f }
    fun add(dx: Float, dy: Float): Boolean {
        x += dx
        y += dy
        return abs(x) > threshold || abs(y) > threshold
    }
}
