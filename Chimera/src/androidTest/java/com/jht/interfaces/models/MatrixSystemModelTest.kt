package com.jht.interfaces.models

import android.net.Uri
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.file.CustomAssetFiles
import com.jht.chimera.lib.common.i18n.LanguageProperty
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.FrameType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
class MatrixSystemModelTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var systemModel : SystemModel
    @Inject lateinit var uiBaseModel: UIBaseModel // TODO - should probably move to own model test
    @Inject lateinit var workoutModel: WorkoutModel // TODO - should probably move to own model test
    private lateinit var lock : Object // TODO kotlin says not to do this, but I don't know the preferred kotlin way yet so this stays

    // how to tell we are in a test - logic on splash screen to disable killing?

    @Before
    fun setup() {
        println("setup")
        hiltRule.inject()
        lock = Object()
    }

    @After
    fun cleanup() {
        println("cleanup")
        systemModel.close()
    }

    @Test
    fun frameTypeTest(){
        // save original setting
        val originalFrame = DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING)

        // testing API GET (no set, yet?)
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.Elliptical_Endurance)
        Assert.assertEquals(FrameType.Elliptical, uiBaseModel.frameType)

        // reset to original setting
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.getFrameTypeFromString(originalFrame))
    }

    @Test
    fun isWattsInaccurateTest(){
        // save original setting
        val originalFrame = DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING)

        // testing API GET (no set, yet?)
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.A30_02)
        Assert.assertEquals(false, workoutModel.wattsDisplayAllowed)
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.Elliptical_Endurance)
        Assert.assertEquals(true, workoutModel.wattsDisplayAllowed)

        // reset to original setting
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.getFrameTypeFromString(originalFrame))
    }

    @Test
    fun distancePrecisionTest(){
        // save original setting
        val original = DataMatrix.getInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT)

        // testing API GET (no set, yet?)
        DataMatrix.putInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT, 6)
        Assert.assertEquals(6, workoutModel.distancePrecision)

        // reset to original setting
        DataMatrix.putInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT, original)
    }

    @Test
    fun languageListTest(){
        // kind of a goofy one, really this is just making sure the interface get() isn't changed without us catching it
        // testing API GET (no set, yet?)
        Assert.assertEquals(LanguageProperty.getSpinnerList(), systemModel.languageList)
    }

    @Test
    fun currentLanguageIndexTest(){
        // save original setting
        val original = DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT)

        // testing
        DataMatrix.putInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT, 3)
        Assert.assertEquals(3, systemModel.currentLanguageIndex.value)

        // reset to original setting
        DataMatrix.putInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT, original)
    }

    @Test
    fun setLanguageTest(){
        // save original setting
        val original = DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT)

        // testing
        systemModel.setLanguage(3)
        // there are more keys changed with changing language (metric/imperial, clock 24/12, etc. but I
        // think just checking this is enough to verify the interface is doing what it should, which is the goal.
        // other tests can verify the correctness of the rest of the language call function)
        Assert.assertEquals(3, DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT))

        // reset to original setting
        systemModel.setLanguage(original) // can be cheeky here and reset using the thing being tested, because if it fails it won't reach this
    }

    @Test
    fun resetLanguageTest(){
        val defaultLanguageInt = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT)

        // testing
        systemModel.setLanguage(if (defaultLanguageInt > 0) defaultLanguageInt-1 else defaultLanguageInt+1) // stay in bounds but change from default
        systemModel.resetLanguage()

        Assert.assertEquals(defaultLanguageInt, DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT))
    }

    @Test
    fun is24HourFormatTest(){
        // save original setting
        val original = DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL)

        // testing API GET (no set, yet?)
        DataMatrix.putBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL, true)
        Assert.assertEquals(true, workoutModel.is24HourFormat)
        DataMatrix.putBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL, false)
        Assert.assertEquals(false, workoutModel.is24HourFormat)

        // reset to original setting
        DataMatrix.putBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL, original)
    }
    @Test
    fun themeUriTest(){ // Listener example
        // save original setting
        val original = DataMatrix.getString(SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING)

        // testing
        DataMatrix.putString(SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING, "test")
        Assert.assertEquals("test", uiBaseModel.themeUri.value)

        var listenerWorking = false
        uiBaseModel.themeUri.listenAndTrigger {
            listenerWorking = it == "listenerTest"
            synchronized(lock) {
                lock.notify()
            }
        }
        DataMatrix.putString(SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING, "listenerTest")
        synchronized(lock) {
            lock.wait(1000) // 1 second wait for listener to fire - when whole test is running sometimes it fails
        }
        Assert.assertEquals(true, listenerWorking)

        // reset to original setting
        DataMatrix.putString(SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING, original)
    }

    @Test
    fun logoUriTest(){
        // save original setting
        val original = DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL)

        // testing
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL, false)
        Assert.assertEquals("", systemModel.logoUri.value)
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL, true)
        val appDir = ContextUtil.getApplicationContext()?.filesDir
        if (appDir != null && appDir.exists()) {
            val logoDir = File(appDir, "settings/theme/logo")
            if (logoDir.exists()) {
                val files = logoDir.listFiles()
                if (files != null && files.isNotEmpty()) {
                    val logoFile = files[0]
                    Assert.assertEquals(Uri.fromFile(logoFile).toString(), systemModel.logoUri.value)
                }
            }
        } else {
            Assert.assertEquals("", systemModel.logoUri.value)
        }

        var listenerWorking = false
        systemModel.logoUri.listenAndTrigger {
            listenerWorking = it == ""
            synchronized(lock) {
                lock.notify()
            }
        }
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL, false)
        synchronized(lock) {
            lock.wait(1000) // 1 second wait for listener to fire - when whole test is running sometimes it fails
        }
        Assert.assertEquals(true, listenerWorking)

        // reset to original setting
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL, original)
    }

    @Test
    fun customBackgroundUriListTest(){
        // save original setting
        val original = DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_BOOL)

        // testing
        val expectedDefaultBackground = CustomAssetFiles.getDefaultBackground()
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_BOOL, false)
        Assert.assertEquals(expectedDefaultBackground, systemModel.customBackgroundUriList.value)
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_BOOL, true)
        Assert.assertNotEquals(expectedDefaultBackground, systemModel.customBackgroundUriList.value)

        var listenerWorking = false
        systemModel.customBackgroundUriList.listenAndTrigger {
            listenerWorking = it == expectedDefaultBackground
            synchronized(lock) {
                lock.notify()
            }
        }
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_BOOL, false)
        synchronized(lock) {
            lock.wait(1000) // 1 second wait for listener to fire - when whole test is running sometimes it fails
        }
        Assert.assertEquals(true, listenerWorking)

        // reset to original setting
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_BOOL, original)
    }

    @Test
    fun customBackgroundTimeSecondsTest(){
        // save original setting
        val original = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT)

        // testing
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT, 24)
        Assert.assertEquals(24, systemModel.customBackgroundTimeSeconds)

        // reset to original setting
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT, original)
    }

    @Test
    fun connectWatchResourceIdTest(){
        // save original setting
        val originalFrame = DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING)

        // TODO - update to use the actual P1 frames once the p1 branch is merged
        // testing
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.Elliptical_Performance)
        synchronized(lock) {
            lock.wait(1000) // 1 second wait, give time for dm callback to execute
        }
        Assert.assertEquals(0, systemModel.connectWatchResourceId)
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.Climbmill_Performance)
        synchronized(lock) {
            lock.wait(1000) // 1 second wait, give time for dm callback to execute
        }
        Assert.assertEquals(R.drawable.connect_watch_climbmill_p1, systemModel.connectWatchResourceId)
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.Treadmill_Performance)
        synchronized(lock) {
            lock.wait(1000) // 1 second wait, give time for dm callback to execute
        }
        Assert.assertEquals(R.drawable.connect_watch_treadmill_p1, systemModel.connectWatchResourceId)
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.UprightBike_Performance)
        synchronized(lock) {
            lock.wait(1000) // 1 second wait, give time for dm callback to execute
        }
        Assert.assertEquals(R.drawable.connect_watch_upright_bike_p1, systemModel.connectWatchResourceId)
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.RecumbentBike_Performance)
        synchronized(lock) {
            lock.wait(1000) // 1 second wait, give time for dm callback to execute
        }
        Assert.assertEquals(R.drawable.connect_watch_recumbent_bike_p1, systemModel.connectWatchResourceId)
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.Ascent_Performance)
        synchronized(lock) {
            lock.wait(1000) // 1 second wait, give time for dm callback to execute
        }
        Assert.assertEquals(R.drawable.connect_watch_ascent_p1, systemModel.connectWatchResourceId)

        // reset to original setting
        FrameConfigManager.getInstance().setFrame(FrameConfigEnum.getFrameTypeFromString(originalFrame))
    }

    @Test
    fun screenTimeoutSecondsTest() {
        // testing API GET
        DataMatrix.putInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT, 45)
        Assert.assertEquals(45, systemModel.screenTimeoutSeconds)
        // testing API SET
        systemModel.screenTimeoutSeconds = 30
        Assert.assertEquals(30, DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT))
    }

}