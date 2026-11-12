package com.jht.chimera.lib.widget.activity

import android.app.Application
import android.os.Bundle
import android.widget.TextView
import com.jht.chimera.R
import dev.b3nedikt.restring.Restring
import dev.b3nedikt.restring.repository.MemoryStringsRepository
import dev.b3nedikt.reword.RewordInterceptor
import dev.b3nedikt.viewpump.ViewPump
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
// A plain Application keeps Hilt and the boot logic in MatrixApplication out of the test.
@Config(application = Application::class)
class RestringActivityTest {

    class TestActivity : RestringActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat)
            super.onCreate(savedInstanceState)
        }
    }

    @Before
    fun setUp() {
        Restring.stringRepository = MemoryStringsRepository()
        // Mirrors MatrixApplication.attachBaseContext
        ViewPump.init(RewordInterceptor)
    }

    private fun inflateConciergeText(): String {
        val activity = Robolectric.buildActivity(TestActivity::class.java).create().get()
        val root = activity.layoutInflater.inflate(R.layout.concierge_button_layout, null)
        return root.findViewById<TextView>(R.id.concierge_btn_text).text.toString()
    }

    @Test
    fun `layout XML text shows the downloaded translation`() {
        Restring.putStrings(Locale.getDefault(), mapOf("request_help_tc" to "[RS] Help"))

        assertEquals("[RS] Help", inflateConciergeText())
    }

    @Test
    fun `layout XML text keeps the bundled string when there is no translation`() {
        val bundled = Robolectric.buildActivity(TestActivity::class.java).create().get()
            .getString(R.string.request_help_tc)

        assertEquals(bundled, inflateConciergeText())
    }
}
