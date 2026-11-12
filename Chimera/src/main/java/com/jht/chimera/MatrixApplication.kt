package com.jht.chimera

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.language.RestringTranslationLoader
import com.jht.datamatrixlib.DataMatrix
import dagger.hilt.android.HiltAndroidApp
import dev.b3nedikt.restring.Restring
import dev.b3nedikt.restring.repository.MemoryStringsRepository
import dev.b3nedikt.reword.RewordInterceptor
import dev.b3nedikt.viewpump.ViewPump
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "MatrixApplication"
private const val START_PROCESS = "com.jht.splashapp"

/** Processes that display translated UI and need the translation files loaded. */
private val TRANSLATION_PROCESSES = setOf("com.jhtp.uiservice", "com.jhtp.virtualactive")
private const val TRANSLATION_PROCESS_PREFIX = "com.jht."

private const val DEBUG_LOGS = false

@HiltAndroidApp
class MatrixApplication : MultiDexApplication(), Application.ActivityLifecycleCallbacks {
    // a CoroutineWorker is another option if work needs to survive process death, but it that case we may need to restart anyway, so not worth it and not really the use case
    // (application scope is automatically cleaned up on process death)
    private val applicationScope = CoroutineScope(
        SupervisorJob() +
        Dispatchers.IO +
        CoroutineExceptionHandler { _, e -> Log.e(TAG, "Startup coroutine failed", e) }
    )

    override fun onCreate() {
        super.onCreate()
        val processName = getProcessName()
        // One-time removal of prefs files persisted by earlier builds that used Restring's
        // default repository; they caused multi-second lookup storms at boot. Runs in every
        // process (near-free when nothing is left to delete; the files are shared app-wide).
        applicationScope.launch(CoroutineName("RestringPrefsCleanup")) {
            RestringTranslationLoader.cleanUpLegacyRestringPrefs(this@MatrixApplication)
        }
        // Must be set before any activity attaches; RestringActivity.attachBaseContext
        // consults it via RestringTranslationLoader.ensureLoaded().
        RestringTranslationLoader.isEnabledInThisProcess = shouldLoadTranslations(processName)
        if (RestringTranslationLoader.isEnabledInThisProcess) {
            // RestringActivity/RestringThemedActivity re-apply layout XML text through Restring with this.
            ViewPump.init(RewordInterceptor)
            //Load translations synchronously on the main thread to prevent concurrent modification exceptions
            //with string lookups from the UI.
            val loaded = RestringTranslationLoader.ensureLoaded()
            if (DEBUG_LOGS) Log.i(TAG, "Restring translations loaded=$loaded in process '$processName'")
        } else {
            if (DEBUG_LOGS) Log.d(TAG, "Skipping translation load in process '$processName'")
        }
        // Register for activity life cycle callbacks for UI service forwarding (since activities can be in different processes, we essentially listen in each process)
        registerActivityLifecycleCallbacks(this) // apparently any attempt to register from anywhere else does not work
        // Check for current process and take any needed actions
        Log.d(TAG, "Application onCreate in process '$processName'")
        if (processName == START_PROCESS) {
            applicationScope.launch(CoroutineName("AsyncBoot")) {
                StartupLogic.runAsyncBootLogic(applicationContext)
            }
        }
    }

    /**
     * Fires in every process when the system configuration changes, including the system-wide
     * locale change made by [com.jht.chimera.lib.common.i18n.LocaleUtil.changeLanguage].
     *
     * The load is intentionally SYNCHRONOUS (~tens of ms, and only when the locale actually
     * changed - the loader no-ops otherwise): this callback runs before the activities'
     * configuration callbacks, and activities declaring configChanges="locale" are not
     * recreated - Compose recomposes immediately via LocalConfiguration and re-reads strings
     * right away. An async load loses that race and nothing triggers a recomposition when it
     * finishes, leaving stale text on screen.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!RestringTranslationLoader.isEnabledInThisProcess) return

        val locale = newConfig.locales.takeIf { !it.isEmpty }?.get(0) ?: return
        val loaded = RestringTranslationLoader.ensureLoaded(locale)
        Log.i(TAG, "Restring translations reloaded=$loaded for locale '$locale'")
    }

    /**
     * Translations are only needed in processes that render translated UI. The splash process
     * ([START_PROCESS]) is excluded as it shows no translatable UI. A null process name
     * (which getProcessName() should never return in practice) safely skips loading.
     */
    private fun shouldLoadTranslations(processName: String?): Boolean {
        return processName != null &&
            processName != START_PROCESS &&
            (processName in TRANSLATION_PROCESSES || processName.startsWith(TRANSLATION_PROCESS_PREFIX))
    }

    /**
     * Route string lookups made through the application context (e.g. [com.jht.chimera.lib.language.StringLookup])
     * through Restring.
     */
    override fun attachBaseContext(base: Context) {
        Restring.stringRepository = MemoryStringsRepository()
        super.attachBaseContext(Restring.wrapContext(base))
    }

    //region ActivityLifecycleCallbacks
    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed ${activity.componentName}")
        // tried creating ActivityLifecycleCallbacks in UI service but they did not trigger, so need to get them to UI service
        // could use broadcast receiver, DM, or something else?
        DataMatrix.putParcelable(UiServiceKeys.EVENT_UI_RESUMED_PARCELABLE, activity.intent)
    }
    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "onActivityPaused ${activity.componentName}")
        DataMatrix.putParcelable(UiServiceKeys.EVENT_UI_PAUSED_PARCELABLE, activity.intent)
    }
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
    //endregion ActivityLifecycleCallbacks

} // END APPLICATION
