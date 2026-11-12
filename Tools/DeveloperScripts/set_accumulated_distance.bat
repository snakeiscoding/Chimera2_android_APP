@echo off
if "%1"=="" goto usage

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_ACCUMULATED_DISTANCE_KM_FLOAT --es type float --es value %1
goto:eof

:usage
echo Usage: %0 ^<distance in kilometers^>
exit/B 1

:eof
