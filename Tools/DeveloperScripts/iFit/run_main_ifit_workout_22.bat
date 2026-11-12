@ECHO OFF

set loop=0
:loop

REM click iFit
adb shell input tap 700 150
REM wait 25 seconds for iFit to load
timeout 25
REM open the main workout shown in the middle of iFit
adb shell input tap 1000 750
REM wait 10 seconds for workout to open
timeout 10
REM start the workout
adb shell input tap 800 400
REM wait 10 seconds
timeout 10
REM accept danger agreement
adb shell input tap 900 650
REM wait in warm up for 60 seconds.. you can adjust this
timeout 60 
REM skip the warm up
adb shell input tap 900 750
REM run workout for 25 mins.. you can adjust this
timeout 1500
REM open exit box
adb shell input tap 0 0 
REM wait 10 seconds
timeout 10
REM close iFit
adb shell input tap 750 600
REM wait 20 seconds
timeout 20
REM let's do it again, adjust how many workouts you want to run
set /a loop=%loop%+1 
if "%loop%"=="100" goto next
goto loop

:next
echo done!