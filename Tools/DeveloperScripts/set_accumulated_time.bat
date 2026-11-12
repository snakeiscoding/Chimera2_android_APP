@echo off
if "%1"=="" goto usage

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_ACCUMULATED_TIME_SECONDS_INT --es type integer --es value %1
goto:eof

:usage
echo Usage: %0 ^<time in seconds^>
exit/B 1

:eof
