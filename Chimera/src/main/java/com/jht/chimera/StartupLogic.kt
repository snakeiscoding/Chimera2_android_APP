package com.jht.chimera

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.jht.androidcommonalgorithms.util.SystemDetection
import com.jht.chimera.lib.common.BootStamp
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.SystemPropertyUtil
import com.jht.chimera.lib.common.app.AndroidSetting
import com.jht.chimera.lib.common.app.IgnoreList
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datetime.DateTimeUtil
import com.jht.chimera.lib.common.log.ConsoleEvent
import com.jht.chimera.lib.common.permission.GrantPermission
import com.jht.chimera.lib.common.testing.InstrumentationTestStatus
import com.jht.chimera.service.ConsoleServicesHelper
import com.jht.chimera.service.sso.SingleSignOnService
import com.jht.datamatrixlib.DataMatrix
import com.jht.utility.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NonNls
import java.io.File
import java.util.Calendar


private const val TAG = "StartupLogic"
private const val STAMP_ROOT_CONFIG = "rootConfig"

/** Our console software start up logic. (Invoked from Application.onCreate) */
object StartupLogic {

    /** operations to run at application boot up that can run in background thread.
     * ORDER MATTERS: the machine-setup clear must precede startServices() — launchApp()
     * is gated on the services-ready flag, which preserves CGC-21804's "clear before
     * first-screen decision" invariant without blocking the splash first frame. */
    suspend fun runAsyncBootLogic(context: Context) = withContext(Dispatchers.IO) {

        ConsoleEvent.logEvent("Initial Boot of ${(if (BuildConfig.DEBUG) "Debug" else "Release")} Version ${BuildConfig.SoftwareVersion} ${BuildConfig.GIT_HASH}")

        ConsoleEvent.logEvent("Console Serial: " + DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING))
        ConsoleEvent.logEvent("Frame Serial: " + DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING))
        ConsoleEvent.logEvent("Club Id: " + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT))
        ConsoleEvent.logEvent("DAPI Environment: " + DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT))
        ConsoleEvent.logEvent("Frame Type: " + DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING))
        ConsoleEvent.logEvent("Console Type: " + ProductConfig.getConsoleType())

        if (SystemDetection.isEmulator()) {
            Log.i(TAG, "EMULATOR DETECTED - Engaging DEMO mode")
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true)
        }

        // Clear machine setup if we are forcing machine setup
        // MUST RUN BEFORE SPLASH DETERMINES FIRST SCREEN = CGC-21804
        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_SHOW_MACHINE_SETUP_BOOL, false)) {
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, false)
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_SHOW_MACHINE_SETUP_BOOL, false)
        }

        // Make sure permissions are granted first in case service logic needs a granted permission
        grantPermissions(context)

        // Start services BEFORE the root-shell work below: service process cold-starts then
        // overlap the multi-second su session instead of queueing behind it. The machine-setup
        // clear above still precedes this call (CGC-21804), and none of the pm/settings
        // commands below are consumed by service startup (they configure OS
        // components — launcher, media scanner, permission dialogs — not our services).
        ConsoleServicesHelper.startServices()
        executeAsRoot(context)
        uninstallOldPackages(context)
        runOtherUtilities()
    }


    /* ===== SUPPORT FUNCTIONS ===== */

    private fun grantPermissions(context: Context){
        GrantPermission.checkGrantSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        GrantPermission.checkGrantSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GrantPermission.checkGrantSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
            GrantPermission.checkGrantSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            GrantPermission.checkGrantSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    private fun updateContentResolver(context: Context){
        val contentResolver = context.contentResolver
        if (contentResolver != null) {
            // enable all the latin keyboard type
            // NOTE for new languages we can use android settings menu to enable the languages, use the our keyboard to flip keyboard language, then run "settings list secure | grep input" to find codes for enabled_input_methods
            AndroidSetting.putString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS,
                "com.android.inputmethod.latin/.LatinIME;1124698716;-756735787;-1337596075;2009405806;1067440414;1872175968;1494081088;1983547218;931682827;758984400;835636643;-1902849005;774684257;-2067235743;-486540198;843948332;242746067;770990173;-2066550842;1244756446;-1818808594;1219821379;529847764;-921088104;118381575;1727731901;"
            ) //NON-NLS
            AndroidSetting.putString(contentResolver, "jht_ignore_intent_list", IgnoreList.sIgnoreIntentList) //NON-NLS
            AndroidSetting.putString(contentResolver, "jht_ignore_activity_list", IgnoreList.sIgnoreActivityList) //NON-NLS
            // enable animations for compose
            if (!InstrumentationTestStatus.isRunningTest && Settings.Global.getInt(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0) == 0) {
                Settings.Global.putInt(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1)
            }
        }
    }

    private fun executeAsRoot(context: Context){

        // Set frame property. Used in automated tests by DQA (Keep running this on every boot in case value changed)
        SystemPropertyUtil.set("persist.jht.frame", DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING))

        // Run the one-time setup commands only when the signature (OS fingerprint + OS build
        // time + software version) differs from the last run — i.e., after an OS or app update.
        val signature = "${Build.FINGERPRINT}|${Build.TIME}|${BuildConfig.SoftwareVersion}"
        Log.d(TAG, "--> Boot signature: $signature")
        val stamp = BootStamp(File(context.filesDir, "boot_stamps"))
        if (stamp.isCurrent(STAMP_ROOT_CONFIG, signature)) {
            Log.d(TAG, "--> Boot config already applied for signature — skipping")
            return
        }

        // NOTE: The following logic will only run once when the OS fingerprint, OS build
        // time, or software version changes.

        updateContentResolver(context)

        val executeAsRoot = ExecuteAsRoot()
        //region Android Settings
        // Re-enable the android launcher.  This is disabled by the updater.
        executeAsRoot.addCommand("pm enable com.android.launcher3")

        // Disable the Android USB permission activity that asks the user permission to use the USB device (ie Dual RS232-HS)
        executeAsRoot.addCommand("pm disable com.android.systemui/com.android.systemui.usb.UsbPermissionActivity")

        // com.android.mtp ReceiverActivity attempts to open DocumentsUI by issuing a view document intent,
        //  which can throw ActivityNotFoundException if nothing handles it, so just block the activity (less quirks than trying to eat it ourselves)
        executeAsRoot.addCommand("pm disable com.android.mtp/.ReceiverActivity")

        // Add logic to prevent the android.process.media from crashing which seems to be triggered with
        // LOCALE_CHANGED handled by MediaScannerReceiver. There's also a BackupManagerService log when this occurs.
        // NOTE that this might be changed in newer Android versions.
        //
        // make sure the backup manager is disabled
        executeAsRoot.addCommand("bmgr enable FALSE")
        // disable the MediaScannerReceiver
        executeAsRoot.addCommand("pm disable com.android.providers.media/.MediaScannerReceiver")

        // turn off sounds
        executeAsRoot.addCommand("settings delete system alarm_alert")
        executeAsRoot.addCommand("settings delete system ringtone")
        executeAsRoot.addCommand("settings delete system notification_sound")
        executeAsRoot.addCommand("settings put system sound_effects_enabled 0")

        // disable the Android OS NTP default logic from running by setting the ntp_server to an empty string
        executeAsRoot.addCommand("settings put global ntp_server \"\"")

        // Some consoles have a fake USB drive that was added during instrumentation testing.
        // Make sure this is gone.
        executeAsRoot.addCommand("rm -rf /sdcard/fake_udisk")

        // attempt to disable chatty expire logs
        SystemPropertyUtil.set("persist.logd.filter", "disable")

        //region Third Party Settings
        // permissions for wechat
        if (!ProductConfig.isProjectOne()) {
            executeAsRoot.addCommand("pm disable com.tencent.mm/com.tencent.mm.pluginsdk.permission.PermissionActivity")
        }
        //endregion

        //region Hardware Specific Settings
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)) {
            // disable addition apk
            executeAsRoot.addCommand("pm disable com.google.android.gms")
            executeAsRoot.addCommand("pm disable com.google.android.gsf")
            executeAsRoot.addCommand("pm disable com.android.vending")

            // disable some browsable intent
            executeAsRoot.addCommand("pm disable com.netease.cloudmusic/.activity.RedirectActivity")
            executeAsRoot.addCommand("pm disable org.chromium.webview_shell")
            executeAsRoot.addCommand("pm disable com.android.chrome")
        }
        else if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)) {
            executeAsRoot.addCommand("pm disable org.chromium.webview_shell")
        }
        else if (ProductConfig.hardware.isVortexBase) {
            // Disable the permission dialog. We could also ask for an OS mod to auto disable permissions if needed.
            executeAsRoot.addCommand("pm disable com.android.permissioncontroller/.permission.ui.GrantPermissionsActivity")
            // Disable lightning browser so it's not one of the defaults android.intent.action.VIEW.
            executeAsRoot.addCommand("pm disable acr.browser.lightning")
            // Disable the rockchip update service
            executeAsRoot.addCommand("pm disable android.rockchip.update.service")
            // Config internal DHCP - can we just make this the default in the OS?
            executeAsRoot.addCommand("am broadcast -a com.jht.eth --es eth1_ip " + ProductConfig.VORTEX_INTERNAL_IP + " --es dhcp_start_address 100 --es dhcp_end_address 200") // could send ourselves if we have context?
            // Disable client from computing window frame and let the WindowManager handle it.
            // Client window computation does not know about the workout panels so it will compute the wrong window frame.
            // Note that newer version of Android forces client window computation so we will need to figure if we need to disable that as a OS mod.
            // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r45:frameworks/base/core/java/android/view/ViewRootImpl.java;l=332
            executeAsRoot.addCommand("setprop persist.debug.local_layout false")
        }

        if(ProductConfig.isRetail() || ProductConfig.platform.equals(ProductConfig.Platform.LCGUI)){
            // Disable BLE local privacy
            ExecuteAsRoot.touch("/data/system/disable_ble_local_privacy.txt")
        }
        //endregion

        // https://source.android.com/docs/core/tests/debug/understanding-logging#log-standards
        // If needed, we could suppress spam logs by hardware.
        // Disable 'rockit' spam logs with level S (suppress). Vendor mentioned they are unable to disable it on their side
        // since it comes from the Rockchip pre-build library.
        executeAsRoot.addCommand("setprop persist.log.tag.rockit S")

        executeAsRoot.execute(false)
        stamp.update(STAMP_ROOT_CONFIG, signature)
    }

    private fun uninstallOldPackages(context: Context){
        // uninstall any old APK (NOTE the TPA cleanup package and future app store should probably handle most of this)
        if (ProductConfig.isRetail()) {
            // if you run "dumpsys package com.google.android.webview | grep versionName" and see 2 packages, there is a side loaded version
            uninstallOldPackage(context, "com.google.android.webview") // remove any side-loaded webview (this package is always present regardless of being side loaded)
        }

        if (ProductConfig.isIfitBlocked()) { // this function already has flavor checks in it
            // actually go so far as to remove ifit is present
            uninstallOldPackage(context, "com.ifit.standalone")
        }

    }

    private fun runOtherUtilities(){
        ProductConfig.checkForceResolution()
        SingleSignOnService.setupIdFiles()

        // Verify the time is not before the build date.
        val buildTime = Calendar.getInstance()
        val now = Calendar.getInstance()
        buildTime.setTime(BuildConfig.BUILD_TIME)
        if (now.before(buildTime)) {
            DateTimeUtil.setDate(
                buildTime.get(Calendar.YEAR),
                buildTime.get(Calendar.MONTH),
                buildTime.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    private fun uninstallOldPackage(context: Context, packageName: @NonNls String) {
        runCatchingCancellable {
            val packageManager = context.packageManager
            packageManager.getPackageInfo(packageName, 0)

            // if we got here, it means the package is installed
            val command: @NonNls String = "pm uninstall $packageName"
            val executeAsRoot = ExecuteAsRoot()
            executeAsRoot.addCommand(command)
            executeAsRoot.execute(false)
        }
    }

} // END Start up object
