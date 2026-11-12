# Build, flavors, and test tasks

Common commands are in `CLAUDE.md`. This covers the rest.

## Flavors

Flavor source sets under `Chimera/src/` (`parker_main`, `vision600e`, `t600xe`, `projectone`,
`chimera_mainMatrixLcgui`) overlay `main` per resource name, so a value in a flavor source set
overrides `main` for that flavor alone. Runtime differences go through `ProductConfig`
(`isParker()`, `isRetail()`, `hasRFID()`, `supportsPtp()`, `hasBumpSensor()`, …) rather than flavor
checks scattered through the code.

## Unit tests

Library modules (`UI:*`, `Core:*`, `System:*`, `Feature:*`) have unflavored `testDebugUnitTest` /
`testReleaseUnitTest`. `:Chimera` is a flavored application module and has only flavored variants of
those tasks. Single class: append `--tests "com.jht.landmarks.LandmarksViewModelTest"`.

## Instrumentation tests

Need a console or emulator over ADB. `buildSrc` registers them under the `tests` task group —
`chimeraMainAppTests`, `chimeraSplashTests`, `chimeraMachineSettingsTests`,
`allChimeraInstrumentationTests`, and `chimeraEmulator*` equivalents. `runInstrumentationTests.sh`
runs the emulator-annotated set directly via `adb shell am instrument`.

## Simulating device state

`Tools/DeveloperScripts/` holds ~60 `.bat` scripts plus feature subfolders (`seca/`, `gymkit/`,
`iFit/`, `myzone/`, `physical_keys/`, `safety_event/`). Most are a single
`adb shell am broadcast -a com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY` writing one
DataMatrix key — that is how you fake state the hardware or a logged-in user would produce: heart
rate, distance, demo mode, bump sensor thresholds, LCB type, units. Try these before wiring up
hardware.

`persistent_off.bat` disables persistent processes so a push no longer needs a reboot; see the PERS
note in `README.md`. `buildSrc` also registers ADB tasks (`simulateGoButton`, `reboot`,
`switchToAndroidLauncher`, …) and log parsers (`parseUserEvents`, `parseTunerLogs`,
`parseUpdateEvents`).
