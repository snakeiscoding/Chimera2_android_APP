@echo off

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key ACTION_IO_PLAY_BEEP --es type event --es value 0