@echo off
adb shell su 0 pm enable com.android.launcher3
adb shell am start -n com.android.launcher/com.android.launcher2.Launcher
adb shell am start -n com.android.launcher3/com.android.launcher3.Launcher
adb shell am start -n com.android.launcher3/com.android.launcher3.uioverrides.QuickstepLauncher
