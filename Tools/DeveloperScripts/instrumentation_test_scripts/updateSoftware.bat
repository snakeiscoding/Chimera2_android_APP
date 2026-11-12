cd ..\..
adb start-server
adb install -r -d build\outputs\apk\chimera_mainMatrixChimera\debug\Chimera-chimera_main-matrix-chimera-debug.apk
adb push Deploy\chimera_assets\. /sdcard/jht
adb install -r -d build\outputs\apk\androidTest\chimera_mainMatrixChimera\debug\Chimera-chimera_main-matrix-chimera-debug-androidTest.apk
timeout 5
adb shell am start -n com.android.launcher3/com.android.launcher3.Launcher
timeout 5

