@echo off

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_P1_ENABLE_DEBUG_VIEW_BOOL --es type boolean --es value false