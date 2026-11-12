@echo off
if "%1"=="" goto usage

set script_path=%1
set script_basename=%~nx1
set script_dir_on_device=/sdcard/tmp/
set script_path_on_device=%script_dir_on_device%/%script_basename%

adb shell mkdir -p /sdcard/tmp/
adb push %script_path% %script_path_on_device%

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key ACTION_RUN_WORKOUT_SCRIPT_STRING --es type string --es value "@%script_path_on_device%"


goto:eof

:usage
echo Usage: %0 ^<script file^>
exit/B 1

:eof
