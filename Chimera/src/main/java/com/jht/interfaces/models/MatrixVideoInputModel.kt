package com.jht.interfaces.models

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.jht.chimera.R
import com.jht.chimera.drivers.audio.AudioSourceInput
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.config.ProductConfig.ConsoleHardware
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.tv.hdmi.ResolutionMonitor
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750ECommand
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750ECommandSender
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EKey
import com.jht.chimera.service.hal.lib.hardware.HdmiIn
import com.jht.data.AppInfo
import com.jht.datamatrixlib.DataMatrix


private const val TAG = "MatrixVideoInputModel"

/** Apps the LG750E tuner can launch, by its app keyword. */
private enum class BoxApps(val keyword: String) {
    YouTube("youtube"),
    Netflix("netflix"),
    Disney("disney"),
    Hulu("hulu"),
}

class MatrixVideoInputModel : VideoInputModel() {
    private val tuner = Lg750ECommandSender.DATA_MATRIX
    private val hdmiMonitor = ResolutionMonitor()

    override fun setInputChangeListener(onInputResolutionChange: (VideoInfo) -> Unit) {
        // only some consoles have HDMI input reporting? maybe we should drop this function and just rely on video stream errors for connection?
        when (ProductConfig.hardware) {
            ConsoleHardware.Parker, ConsoleHardware.Parker_3,
            ConsoleHardware.UltraThin,
                ConsoleHardware.Chimera_2 -> {}
            else -> return
        }
        hdmiMonitor.setOnHdmiInfoChangedListener { oldResolution, newResolution ->
            Log.d(TAG, "HDMI change $oldResolution $newResolution")
            val info = if (newResolution != null) MatrixVideoInfo(newResolution.width, newResolution.height, newResolution.isConnected) else VideoInfo()
            onInputResolutionChange(info)
        }
        // provide current on call
        val current = hdmiMonitor.currentHdmiInfo
        val info = if (current != null) MatrixVideoInfo(current.width, current.height, current.isConnected) else VideoInfo()
        onInputResolutionChange(info)
    }

    override fun applyVideoInput(): VideoInput {
        // select input based on settings? should be method or just do it at model construction?
        if (ProductConfig.supportsHdmiSwitching()) {
            // Select the HDMI port we will use. (WHY IS THIS DIFFERENT FROM CHIMERA?)
            Log.d(TAG, "initHDMI: call switchPort")
            // TODO - how would UI indicate tuner vs user input? how do we know to pass tuner vs null?
            val sourceIsUser = false
            val source = if (sourceIsUser) null else SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING))
            val hdmiSource = ProductConfig.getHdmiInput(source)
            /*val hdmiSource = if(ProductConfig.platform.equals(ProductConfig.Platform.LCGUI)) 2
                else if (SettingKeys.TVSource.valueOf(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)) == SettingKeys.TVSource.KM4) 2
                else 3*/
            HdmiIn.getInstance().switchPort(hdmiSource)
        }
        hdmiMonitor.startMonitoring() // make sure monitor is running
        // chimera1, parker1 - all only list one camera of id "0" ?
        return when (ProductConfig.hardware) { // should probably be abstracted into a console hardware class?
            ConsoleHardware.Vortex, ConsoleHardware.VortexLite ->
                VideoInput(VideoInputType.TV_VIEW, "com.example.partnersupportsampletvinput/.SampleTvInputService/HW0")
            else -> VideoInput() // default first camera
        }
    }

    override val externalAppList: List<AppInfo> get() {
        val context = ContextUtil.getApplicationContext()
        return if (context == null) listOf() else BoxApps.entries.mapNotNull {
            when (it) {
                BoxApps.Netflix -> AppInfo(context.getString(R.string.netflix_tc), it.keyword, getDrawable(context, R.drawable.ic_app_button_netflix))
                BoxApps.Disney -> AppInfo(context.getString(R.string.disney_plus_tc), it.keyword, getDrawable(context, R.drawable.ic_app_button_disney))
                BoxApps.Hulu -> AppInfo(context.getString(R.string.hulu_tc), it.keyword, getDrawable(context, R.drawable.ic_app_button_hulu))
                BoxApps.YouTube -> AppInfo(context.getString(R.string.youtube_tc), it.keyword, getDrawable(context, R.drawable.ic_app_button_youtube))
                else -> null
            }
        }
    }
    private fun getDrawable(context: Context, resId: Int): Drawable = ContextCompat.getDrawable(context, resId) ?: ColorDrawable(Color.WHITE)

    override fun launchExternalApp(name: String) {
        tuner.send(Lg750ECommand.LaunchApp(name))
    }

    override fun sendKey(key: RemoteKeys, count: Int) {
        val code = when (key) {
            RemoteKeys.DirectionLeft -> Lg750EKey.LEFT
            RemoteKeys.DirectionRight -> Lg750EKey.RIGHT
            RemoteKeys.DirectionUp -> Lg750EKey.UP
            RemoteKeys.DirectionDown -> Lg750EKey.DOWN
            RemoteKeys.Back -> Lg750EKey.BACK
            else -> null
        }
        code?.let { tuner.send(Lg750ECommand.KeyPress(it, count)) }
    }

    override fun moveMouseTo(x: Float, y: Float) {
        tuner.send(Lg750ECommand.MoveMouse(x, y))
    }

    override fun clickMouse() {
        tuner.send(Lg750ECommand.ClickMouse)
    }

    override fun clearUserData() {
        tuner.send(Lg750ECommand.ClearUserData)
    }

    override fun setSoftAp(ssid: String, pw: String) {
        tuner.send(Lg750ECommand.SetSoftAp(ssid, pw))
    }

    override fun setLocale(localeCode: String) {
        tuner.send(Lg750ECommand.SetLocale(localeCode))
    }

    override fun close() {
        hdmiMonitor.stopMonitoring()
        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal) // reset audio selection
    }
}

// some consoles can read invalid resolutions but we maintain a "connected" bool? but sometimes that bool is wrong
class MatrixVideoInfo(width: Int, height: Int, private val connected: Boolean) : VideoInfo(width, height) {
    override fun isValid(): Boolean = super.isValid() && connected
}
