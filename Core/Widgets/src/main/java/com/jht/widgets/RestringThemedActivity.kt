package com.jht.widgets

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.ViewPumpAppCompatDelegate
import com.jht.theme.ThemedActivity
import dev.b3nedikt.restring.Restring

/**
 * [ThemedActivity] with Restring dynamic translations.
 *
 * Routes all string resource lookups made through the activity context - including Compose's
 * stringResource(), which reads LocalContext.current.resources - through [Restring], so
 * translations loaded from the console's translation JSON files override the bundled strings.
 *
 * Inflation reads layout XML text without a resource lookup, so the ViewPump delegate re-applies
 * it through Restring.
 *
 * This shim exists because [ThemedActivity] lives in the prebuilt ConsoleResources artifact
 * and cannot be modified here. Compose UI activities should extend this class instead of
 * [ThemedActivity] directly.
 *
 * [Restring.wrapContext] is a documented no-op until Restring is initialized (done in the
 * Application class), so this is safe even if an activity starts first.
 */
open class RestringThemedActivity : ThemedActivity() {

    private var viewPumpDelegate: AppCompatDelegate? = null

    override fun getDelegate(): AppCompatDelegate =
        viewPumpDelegate ?: ViewPumpAppCompatDelegate(super.getDelegate(), this).also { viewPumpDelegate = it }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Restring.wrapContext(newBase))
    }
}
