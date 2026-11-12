@ECHO OFF

rem This may be obsoleted by run_ifit_workouts.sh,
rem which can do cooldowns and automatic logins
rem and is all-around better.

echo Hey, do you want a more up-to-date testing script?  Try run_ifit_workouts.sh, instead. >&2

if not defined sleep_cmd set sleep_cmd=C:\Windows\System32\timeout.exe

if not defined screen_size (echo Set screen_size to "10", "16", or "22" to run this script >&2 & exit /B 1)

setlocal

set loop_max=10000
set loop=0

set screen_size=10

if "%screen_size%"=="10" goto init_10inch_positions
if "%screen_size%"=="22" goto init_22inch_positions

echo Oh no, variables not set for screen_size=%screen_size >&2
exit /B 1




:init_10inch_positions
set can_skip_stop_workout=0
set launch_ifit_pos=512 256
set open_main_ifit_workout_pos=512 384
set start_ifit_workout_pos=300 200
set accept_danger_agreement_pos=512 420
set skip_warmup_pos=512 450
rem pause button
set pause_workout_pos=1000 550
rem Stop button that appears aftyer you pause
set stop_workout_pos=1000 500
set exit_workout_pos=0 0
set confirm_exit_pos=420 380
goto loop

:init_22inch_positions
set can_skip_stop_workout=1
set launch_ifit_pos=700 150
set open_main_ifit_workout_pos=1000 750
set start_ifit_workout_pos=800 400
set accept_danger_agreement_pos=900 650
set skip_warmup_pos=900 750
set exit_workout_pos=0 0
set confirm_exit_pos=750 600
goto loop




:loop

echo Launch iFit
adb shell input tap %launch_ifit_pos%

%sleep_cmd% 30

echo Select main workout
adb shell input tap %open_main_ifit_workout_pos%

%sleep_cmd% 10

echo Start workout
adb shell input tap %start_ifit_workout_pos%

%sleep_cmd% 10

echo Yeah yeah we agree to the danger
adb shell input tap %accept_danger_agreement_pos%

%sleep_cmd% 60 

echo Skip warmup
adb shell input tap %skip_warmup_pos%

echo Let workout run for a while
%sleep_cmd% 1440

if "%can_skip_stop_workout%"=="1" goto exit_workout

rem On 10 inch consoles, the back button isn't visible during the workout;
rem we have to pause and stop the workout before we can quit iFit.

echo Pause workout
adb shell input tap %pause_workout_pos%

%sleep_cmd% 4

echo Stop workout
adb shell input tap %stop_workout_pos%

%sleep_cmd% 10

:exit_workout

echo Exit workout by hitting the back button
adb shell input tap %exit_workout_pos%

%sleep_cmd% 4

echo Confirm exit
adb shell input tap %confirm_exit_pos%

%sleep_cmd% 20

set /a loop=%loop%+1 
if "%loop%"=="%loop_max%" goto next
goto loop

:next
echo done!
