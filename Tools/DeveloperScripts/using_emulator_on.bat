@echo off

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key USING_ANDROID_EMULATOR_BOOL --es type boolean --es value true