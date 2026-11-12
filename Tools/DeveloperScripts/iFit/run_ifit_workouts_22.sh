#!/bin/bash
# This targets the 22" console.

LOOP_MAX=10000
LOOP=0
SCREEN_SIZE=22

# 22" properties
can_skip_stop_workout=0
launch_ifit_pos="700 150"
open_main_ifit_workout_pos="1000 750"
start_ifit_workout_pos="630 450"
accept_danger_agreement_pos="900 650"
skip_warmup_pos="900 750"
# pause button
pause_workout_pos="1850 950"
# Stop button that appears after you pause
stop_workout_pos="1850 950"
exit_workout_pos="0 0"
confirm_exit_pos="750 600"

# Launch the ifit app
echo Launch iFit
adb shell input tap $launch_ifit_pos
sleep 40

# echo Done for now

while [ $LOOP -lt $LOOP_MAX ]; do
    echo Starting workout $LOOP of $LOOP_MAX...

    echo Select main workout
    adb shell input tap $start_ifit_workout_pos
    sleep 15

    echo Start workout
    adb shell input tap 630 450
	adb shell input tap 625 377
    sleep 11

    echo Accept Warning
    adb shell input tap $accept_danger_agreement_pos
    # sleep 60
    sleep 15

    echo Skip warmup
    adb shell input tap $skip_warmup_pos


    echo Let workout run for a while
    sleep 1440
    # sleep 60
    # sleep 60

    echo Expose the start cooldown button
    adb shell input tap 745 940
    sleep 2

    echo Click Start cooldown button
    adb shell input tap 950 950
    sleep 15

    echo End the cooldown
    adb shell input tap 950 730
    sleep 20

    echo Click the finish button
    adb shell input tap 370 340
    # sleep 60
    sleep 15

    # Increment the loop
    let LOOP=LOOP+1
done

echo "Done"
