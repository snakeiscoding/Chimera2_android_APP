@echo off

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_MY_ZONE_CONNECTED_BOOL --es type boolean --es value true