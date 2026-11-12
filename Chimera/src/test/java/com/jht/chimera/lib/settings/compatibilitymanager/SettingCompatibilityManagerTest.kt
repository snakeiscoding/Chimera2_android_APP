package com.jht.chimera.lib.settings.compatibilitymanager

import com.jht.chimera.lib.settings.compatibilitymanager.model.CompatibilityPolicy
import com.jht.chimera.lib.settings.compatibilitymanager.model.ValueConstraint
import com.jht.chimera.lib.settings.compatibilitymanager.model.VersionConstraint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingCompatibilityManagerTest {
    private lateinit var compatibilityManager: SettingCompatibilityManager

    @Before
    fun setup() {
        compatibilityManager = SettingCompatibilityManager(
            listOf(
                CompatibilityPolicy(
                    key = "DATA_TV_SOURCE_STRING",
                    value = ValueConstraint.of("KM6"),
                    sourceVersion = VersionConstraint.any(),
                    targetVersion = VersionConstraint.before(2),
                    compatible = false
                ),

                CompatibilityPolicy(
                    key = "DATA_ADB_OVER_WIFI_BOOL",
                    value = ValueConstraint.of(false),
                    sourceVersion = VersionConstraint.any(),
                    targetVersion = VersionConstraint.any(),
                    compatible = true
                ),

                CompatibilityPolicy(
                    key = "DATA_ADB_OVER_WIFI_BOOL",
                    value = ValueConstraint.any<Any?>(),
                    sourceVersion = VersionConstraint.any(),
                    targetVersion = VersionConstraint.any(),
                    compatible = false
                )
            )
        )
    }

    @Test
    fun `test km6 tv source export to supported console`() {
        assertTrue(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "KM6", 2, 2))
        assertTrue(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "KM6", 2, 4))
        assertTrue(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "KM6", 1, 3))
    }

    @Test
    fun `test km6 tv source export to unsupported console`() {
        assertFalse(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "KM6", 2, 1))
        assertFalse(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "KM6", 1, -1))
    }

    @Test
    fun `test other tv sources can export to unsupported console`() {
        assertTrue(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "US8", 2, 1))
        assertTrue(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "US6", 2, 1))
        assertTrue(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "US7", 2, 1))
        assertTrue(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "US5", 2, 1))
        assertTrue(compatibilityManager.isSettingCompatible("DATA_TV_SOURCE_STRING", "JP0", 2, 1))
    }

    @Test
    fun `test policy to never export adb over wifi`() {
        assertFalse(compatibilityManager.isSettingCompatible("DATA_ADB_OVER_WIFI_BOOL", true, 1, 5))
        assertFalse(compatibilityManager.isSettingCompatible("DATA_ADB_OVER_WIFI_BOOL", false, 1, 5))
        assertFalse(compatibilityManager.isSettingCompatible("DATA_ADB_OVER_WIFI_BOOL", true, 5, 1))
        assertFalse(compatibilityManager.isSettingCompatible("DATA_ADB_OVER_WIFI_BOOL", false, 5, 1))
        assertFalse(compatibilityManager.isSettingCompatible("DATA_ADB_OVER_WIFI_BOOL", "true", 1, 1))
        assertFalse(compatibilityManager.isSettingCompatible("DATA_ADB_OVER_WIFI_BOOL", "false", 1, 1))
    }

    @Test
    fun `test always prioritize non-compatible policies over compatible ones`() {
        assertFalse(compatibilityManager.isSettingCompatible("DATA_ADB_OVER_WIFI_BOOL", false, 1, 1))
    }
}