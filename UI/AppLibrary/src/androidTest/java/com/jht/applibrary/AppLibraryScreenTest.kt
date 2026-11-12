package com.jht.applibrary

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jht.data.AppInfo
import com.jht.theme.defaultDynamicTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLibraryScreenTest {

    // Rule to manage Compose component testing
    @get:Rule
    val composeTestRule = createComposeRule()

    // Context for resource access
    private lateinit var context: Context

    // Setup before each test
    @Before
    fun setup() {
        // Get target context for resource resolution
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    // Helper to generate test app data
    private fun createTestApps(count: Int, locked: Boolean = false): List<AppInfo> {
        return List(count) {
            AppInfo(
                title = "App $it", // Generate unique app name
                uuid = it.toString(), // Unique identifier
                icon = ContextCompat.getDrawable(context, R.drawable.ic_app_button_x)!!, // Default icon
                locked = locked // Lock state control
            )
        }
    }

    /** Test pagination logic - verify grid splits apps into pages */
    @Test
    fun appGrid_showsMultiplePagesWhenNeeded() {
        // 22 apps → 2 pages (21 apps/page in 7x3 grid)
        val apps = createTestApps(22)

        // Render component with test data
        composeTestRule.setContent {
            AppLibraryScreen(
                appInfoList = apps,
                theme = defaultDynamicTheme,
                onAppClick = {},
                onForceLaunch = {}
            )
        }

        // Custom matcher to find pager dots by test tag prefix
        val pagerDotMatcher = SemanticsMatcher("PagerDot_ prefix matcher") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("PagerDot_") ?: false
        }

        // Verify exactly 2 page indicator dots exist
        composeTestRule.onAllNodes(pagerDotMatcher).assertCountEquals(2)
    }

    /** Test app click functionality for unlocked apps */
    @Test
    fun clickingUnlockedApp_triggersOnAppClick() {
        val apps = createTestApps(1)
        var clickedApp: AppInfo? = null

        // Render with click listener
        composeTestRule.setContent {
            AppLibraryScreen(
                appInfoList = apps,
                onAppClick = { clickedApp = it }
            )
        }

        // Find and click app button using test tag pattern
        composeTestRule.onNodeWithTag("AppIcon:0:Button").performClick()

        // Verify callback received correct app data
        assertEquals("Clicked app should match expected", apps[0], clickedApp)
    }

    /** Test locked app behavior - show upgrade dialog */
    @Test
    fun clickingLockedApp_showsUpgradeDialog() {
        // Create locked app instance
        val lockedApp = createTestApps(1, locked = true)

        composeTestRule.setContent {
            AppLibraryScreen(
                appInfoList = lockedApp,
                theme = defaultDynamicTheme
            )
        }

        // Perform click action
        composeTestRule.onNodeWithTag("AppIcon:0:Button")
            .performClick()

        // Verify dialog title exists using string resource
        composeTestRule.onNodeWithText(context.getString(R.string.upgrade_unavailable_tc))
            .assertExists()
    }

    /** Test SSO confirmation dialog visibility */
    @Test
    fun ssoPrompt_showsConfirmationDialog() {
        val ssoApp = createTestApps(1).first()
        // Control dialog state directly
        val ssoPrompt = mutableStateOf<AppInfo?>(ssoApp)

        composeTestRule.setContent {
            AppLibraryScreen(
                appInfoList = emptyList(),
                ssoPrompt = ssoPrompt,
                theme = defaultDynamicTheme
            )
        }

        // Verify dialog buttons using string resources
        composeTestRule.onNodeWithText(context.getString(R.string.yes_tc)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.no_tc)).assertExists()
    }
}