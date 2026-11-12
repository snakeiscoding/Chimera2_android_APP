package com.jht.chimera.lib.widget.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.ViewPumpAppCompatDelegate
import com.jht.chimera.lib.language.RestringTranslationLoader
import dev.b3nedikt.restring.Restring

/**
 * Base activity that routes string resource lookups through [Restring], so translations loaded
 * from /sdcard/jht/translation by [com.jht.chimera.lib.language.RestringTranslationLoader]
 * override the bundled strings. Inflation reads android:text/hint without that lookup, so the
 * ViewPump delegate re-applies them through Restring.
 *
 * [Restring.wrapContext] is a no-op if Restring has not been initialized yet, so this is
 * safe even if an activity starts before [Restring.init] has run.
 */
open class RestringActivity : AppCompatActivity() {

    private var viewPumpDelegate: AppCompatDelegate? = null

    override fun getDelegate(): AppCompatDelegate =
        viewPumpDelegate ?: ViewPumpAppCompatDelegate(super.getDelegate(), this).also { viewPumpDelegate = it }

    override fun attachBaseContext(newBase: Context) {
        // Guarantee the active locale's translations are in memory before this activity
        // inflates anything: after a locale change, activity recreation can win the race
        // against the application's async reload. No-op (volatile read) when already loaded,
        // ~tens of ms once per language change otherwise, and disabled in processes that
        // do not display translated UI.
        RestringTranslationLoader.ensureLoaded()
        super.attachBaseContext(Restring.wrapContext(newBase))
    }
}
