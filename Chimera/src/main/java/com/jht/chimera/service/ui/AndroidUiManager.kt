package com.jht.chimera.service.ui

import android.app.AlertDialog
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ListView
import android.widget.RelativeLayout
import com.jht.chimera.R
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.i18n.LanguageProperty
import com.jht.chimera.lib.common.i18n.LocaleUtil
import com.jht.chimera.lib.widget.listview.StringListViewAdapter
import com.jht.datamatrixlib.DataMatrix

private const val ACTION_SWITCH_KEYBOARD = "com.jht.switch_keyboard" //NON-NLS
private const val TAG = "AndroidUiManager"

/**
 * Class to handle all of the "Android specific UI things we are doing that seem spread about"
 * - Ed Boyer 2024
 * Android Specific UI things it handles:
 * 1.) disabling/enabling the android launcher [processWorkoutRunState]
 * 2.) OS MOD for intercepting keyboard language change [registerHandleKeyboardBroadcast],[unregisterHandleKeyboardBroadcast]
 * 3.) clearing the android clipboard [clearClipboard]
 */
class AndroidUiManager(
    private val applicationContext: Context,
) {
    private var previousRunState = WorkoutKeys.Constants.RUN_STATE_STOP

    /**
     * method to be called by the service when it starts
     */
    fun start() {
        registerHandleKeyboardBroadcast()
    }

    /**
     * method to be called by the service when it stops
     */
    fun stop() {
        unregisterHandleKeyboardBroadcast()
        clearClipboard()
    }

    /**
     * method to be called by the service when it resets, for example the splash screen trigger.
     * Needs to hook into an adapter for HALKeys.ACTION_SPLASH_RESET
     */
    fun reset() {
        clearClipboard()
    }

    /**
     * Handles any changes to the workout run state from the system and runs logic to enable or
     * disable the android launcher.
     * Needs to hook into an adapter for WorkoutKeys.DATA_RUN_STATE_INT
     */
    fun processWorkoutRunState(workoutRunState: Int) {
        if (previousRunState == WorkoutKeys.Constants.RUN_STATE_STOP && workoutRunState == WorkoutKeys.Constants.RUN_STATE_RUNNING) {
            // disable the android launcher when the workout runs so 3rd party app cannot add a shortcut
            setEnableAndroidLauncher(DataMatrix.getBoolean(MainAppKeys.DATA_RUNNING_INSTRUMENTATION_TEST_BOOL))
        } else if (workoutRunState == WorkoutKeys.Constants.RUN_STATE_STOP) {
            // re-enable the android launcher
            setEnableAndroidLauncher(true)
        }
        previousRunState = workoutRunState
    }

    private fun setEnableAndroidLauncher(enable: Boolean) {
        Thread {
            val executeAsRoot = ExecuteAsRoot()
            executeAsRoot.addCommand("pm " + (if (enable) "enable" else "disable") + " com.android.launcher3")
            executeAsRoot.execute()
        }.also { it.name = "ToggleLauncher" }.start()
    }


    // OS MOD for intercepting keyboard language change
    private val handleKeyboardBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "keyboard language change OS Mod intercept received")
            val alertDialogBuilder = AlertDialog.Builder(applicationContext)
            val inflater = applicationContext.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val dlgLayout = inflater.inflate(R.layout.select_keyboard, null) as RelativeLayout
            alertDialogBuilder.setView(dlgLayout)
            val listView = dlgLayout.findViewById<ListView>(R.id.listView)
            val languageList = LanguageProperty.getSpinnerList()
            val adapter = StringListViewAdapter(applicationContext, R.layout.string_list_view_item, R.id.txtStringItem, languageList)
            listView.setAdapter(adapter)

            // create alert dialog
            val alertDialog = alertDialogBuilder.create()
            alertDialog.setCanceledOnTouchOutside(true)
            alertDialog.window?.setType(WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG)
            dlgLayout.findViewById<View>(R.id.btn_close).setOnClickListener { v: View? -> alertDialog.cancel() }
            listView.onItemClickListener =
                AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                    val lp = LanguageProperty.getLanguagePropertyFromIndex(position)
                    LocaleUtil.changeKeyboard(applicationContext, lp.locale)
                    alertDialog.cancel()
                }

            // show it
            alertDialog.show()
        }
    }

    private fun registerHandleKeyboardBroadcast() {
        applicationContext.registerReceiver(handleKeyboardBroadcast, IntentFilter(ACTION_SWITCH_KEYBOARD))
        Log.d(TAG, "keyboard language change intercept OS Mod receiver registered")
    }

    private fun unregisterHandleKeyboardBroadcast() {
        try {
            applicationContext.unregisterReceiver(handleKeyboardBroadcast)
            Log.d(TAG, "keyboard language change intercept OS Mod receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Exception unregistering keyboard language change intercept OS Mod receiver\n${e.stackTrace}")
        }
    }

    private fun clearClipboard() {
        // parker crash> https://stackoverflow.com/a/12946228 <- leftover comment, keeping in case issue arises again, however unlikely
        val clipService = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipService != null) {
            val clipData = ClipData.newPlainText("", "") // empty the clipboard
            clipService.setPrimaryClip(clipData)
        }
    }
}