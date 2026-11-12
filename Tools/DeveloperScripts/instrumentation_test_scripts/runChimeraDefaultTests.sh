#!/usr/bin/env bash
adb shell am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.Default com.jht.chimera.test/androidx.test.runner.AndroidJUnitRunner
