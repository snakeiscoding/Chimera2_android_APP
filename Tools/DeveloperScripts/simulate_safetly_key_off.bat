@echo off

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_SIMULATED_ESTOP_BOOL --es type boolean --es value false