#!/usr/bin/env bash
adb shell am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.MainApp com.jht.chimera.test/.runner.MainAppTestRunner
adb shell am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.MachineSettings com.jht.chimera.test/.runner.MachineSettingsTestRunner
adb shell am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.MachineSetup com.jht.chimera.test/.runner.MachineSetupTestRunner
adb shell am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.SplashApp com.jht.chimera.test/.runner.SplashAppTestRunner
adb shell am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.Default com.jht.chimera.test/androidx.test.runner.AndroidJUnitRunner

