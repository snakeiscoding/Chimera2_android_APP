#!/bin/bash

adb shell am start -n com.android.launcher/com.android.launcher2.Launcher
sleep 2
adb shell am start -n com.android.launcher3/com.android.launcher3.Launcher
sleep 2
adb shell am start -n com.android.launcher4/com.android.launcher4.Launcher
sleep 2
