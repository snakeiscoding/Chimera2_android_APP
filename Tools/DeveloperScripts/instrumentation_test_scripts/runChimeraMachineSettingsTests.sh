#!/usr/bin/env bash
adb shell am instrument -w -e annotation com.jht.chimera.test.annotations.platform.Chimera,com.jht.chimera.test.annotations.platform.Emulator,com.jht.chimera.test.annotations.runner.MachineSettings com.jht.chimera.test/.runner.MachineSettingsTestRunner
