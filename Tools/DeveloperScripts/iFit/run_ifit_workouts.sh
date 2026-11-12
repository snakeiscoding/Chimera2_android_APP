#!/bin/bash
# This targets the 16" console.

set -euo pipefail

# Killing all subprocesses is actually somewhat tricky
# See https://stackoverflow.com/questions/2618403/how-to-kill-all-subprocesses-of-shell
# I am testing on Windows, where killing (MinGW?) bash seems to kill all background jobs anyway,
# so this approach might not fully work:
trap 'subprocs="$(jobs -p)" ; if [ -n "$subprocs" ] ; then echo "Killing subprocesses: $subprocs" >&2 && kill $subprocs ; fi' SIGTERM SIGHUP EXIT

# Configurable parameters:

workout_duration=600
max_workout_iterations=10000
do_taps=1
do_login=0
do_cooldown=0
do_accept_unsaved=0
log_tag="$(basename "$0")"
# Log format can contain:
#   %0 - log tag; by default, the name of this script (probably "run_ifit_workouts.sh")
#   %m - the original message
#   %d - ISO8601-formatted date
#   %a - date in logcat format, e.g. "03-05 11:16:06.123"
#   %q - quiet--if log format is just "%q", don't print logs (this is what --quiet does)
#   %n - a newline character
#   %p - a percent character
#   %c - imitation logcat output (same as '%a  xxxx  xxxx I %0: %m');
#        may be useful if you want a single log file showing automation and logs from the device!
log_format="%m"
do_prompt_for_metadata=0
do_start_dmesg=0
do_start_logcat=0
do_duplicate_automation_info_to_stderr=1
# If reopening stdout to log directory
log_dir=
metadata=()

# Other local variables:

completed_workout_iterations=0
usage_errors=()

while [[ $# > 0 ]]
do
    arg="$1"
    case $arg in
        --log-to-dir)
            # Write to log files in the specified directory.
            # Useful if you don't want to have to manually come up with names for your log files.
            log_dir="$2"
            shift
            ;;
        --prompt-for-metadata)
            do_prompt_for_metadata=1
            ;;
        --metadata)
            metadata+=("$2: $3")
            shift
            shift
            ;;
        --start-dmesg)
            do_start_dmesg=1
            ;;
        --start-logcat)
            do_start_logcat=1
            ;;
        --dry-run)
            # For testing this script without actually sending adb tap commands
            do_taps=0
            ;;
        --log-format)
            # See log format string, above
            log_format="$2"
            shift
            ;;
        --log-tag)
            # Fake 'tag' for logcat imitation output ('%0')
            log_tag="$2"
            shift
            ;;
        --quiet)
            # Don't log
            log_format="%q"
            ;;
        --iterations)
            max_workout_iterations="$2"
            shift
            ;;
        --workout-duration)
            # Amount of time to let workouts run, in seconds
            workout_duration="$2"
            shift
            ;;
        --auto-login)
            do_login=1
            ;;
        --cooldown)
            do_cooldown=1
            ;;
        --accept-unsaved-workouts)
            # On frames where the user needs to physically push pedals to make distance pass,
            # you might need to click 'okay' to accept short workouts not being saved.
            do_accept_unsaved=1
            ;;
        --screen-size)
            screen_size="$2"
            shift
            ;;
        --duplicate-automation-info-to-stderr)
            # If you're redirecting to some log file,
            # you might want automation info also going to stderr
            # so you can notice misclicks.
            do_duplicate_automation_info_to_stderr=1
            ;;
        *)
            usage_errors+=("Unrecognized argument: $arg")
            ;;
    esac
    shift
done

# Prompts are sent to STDERR
# so that you can redirect stdout to a log file

prompt_for_metadata() {
    md_name="$1"
    md_description="$2"
    read -p "${md_description}> " md_value >&2
    if [ -n "${md_value}" ] ; then
        metadata+=("${md_name}: ${md_value}")
    fi
}

if [ "$do_prompt_for_metadata" != "0" ] ; then
    if [ -z "${screen_size+x}" ] ; then
        read -p "Screen size (inches) (10, 16, or 22)> " screen_size >&2
    fi
    prompt_for_metadata "lcb-type-code" "LCB type code (hex) (e.g. '08')"
    prompt_for_metadata "frame-serial-number" "Frame serial number"
    prompt_for_metadata "parker-version" "Parker version"
    prompt_for_metadata "os-version" "OS version"
    prompt_for_metadata "ifit-version" "iFit version"
    read -p "Hit enter when ready!"
fi

metadata+=("script-urn: x-git-object:$(git hash-object "$0")")
metadata+=("workout-duration: ${workout_duration}")
metadata+=("actually-tapping: ${do_taps}")
metadata+=("doing-login: ${do_login}")
metadata+=("doing-cooldown: ${do_cooldown}")

if [ -z "${screen_size+x}" ] ; then
    usage_errors+=("--screen-size unspecified (can also be set with screen_size env. var)")
fi

if [ ${#usage_errors[@]} -ne 0 ] ; then
    echo 'Oh no, you invoked this script wrongly!' >&2
    for usage_error in "${usage_errors[@]}" ; do
        echo "  ${usage_error}" >&2
    done
    exit 1
fi

if [ "$screen_size" == 10 ]; then
    can_skip_stop_workout=0
    launch_ifit_pos="512 256"
    open_main_ifit_workout_pos="512 384"
    continue_series_pos="300 230"
    start_ifit_workout_pos="300 180"
    accept_danger_agreement_pos="512 420"
    skip_warmup_pos="512 450"

    arm_cooldown_pos="512 300"
    start_cooldown_pos="512 462"
    end_cooldown_pos="512 462"
    finish_pos="256 240"
    finish_pos2="256 300"

    # Pause button
    pause_workout_pos="1000 550"
    # Stop button that appears aftyer you pause
    stop_workout_pos="1000 500"
    not_saved_okay_pos="750 405"
    exit_workout_pos="0 0"
    confirm_exit_pos="420 380"
elif [ "$screen_size" == 16 ]; then
    can_skip_stop_workout=1
    launch_ifit_pos="512 256"
    open_main_ifit_workout_pos="682 390"
    start_ifit_workout_pos="282 194"
    accept_danger_agreement_pos="690 523"
    skip_warmup_pos="699 558"
    arm_cooldown_pos="681 484"
    start_cooldown_pos="688 634"
    end_cooldown_pos="680 580"
    finish_pos="265 462"
    finish_pos2="265 462"
    
    # Pause button
    pause_workout_pos="TODO"
    # Stop button that appears after you pause
    stop_workout_pos="TODO"
    exit_workout_pos="TODO"
    confirm_exit_pos="TODO"
elif [ "$screen_size" == 22 ]; then
    can_skip_stop_workout=1
    launch_ifit_pos="700 150"
    open_main_ifit_workout_pos="1000 750"
    start_ifit_workout_pos="800 400"
    accept_danger_agreement_pos="900 650"
    skip_warmup_pos="900 750"
    exit_workout_pos="0 0"
    confirm_exit_pos="750 600"

    echo "Oh no, this script needs a little fixing-up to work on 22-inch consoles" >&2
    exit 1
else
    echo "Oh no, button positions not known for ${screen_size}-inch screen; aborting" >&2
    exit 1
fi


ekko() {
    if [ "$log_format" != "%q" ] ; then
        output="$log_format"
        output="${output//%c/%a  xxxx  xxxx I %0: %m}"
        output="${output//%n/$'\n'}"
        output="${output//%0/$log_tag}"
        output="${output//%d/$(date --iso-8601=seconds)}"
        output="${output//%a/$(date '+%m-%d %H:%M:%S.%3N')}"
        output="${output//%p/%}"
        # Since the message may contain any characters, replace it last:
        echo "${output//%m/$@}"
    fi
    if [ "$do_duplicate_automation_info_to_stderr" != "0" ] ; then
        echo "$@" >&2
    fi
}

sleap() {
    ekko "Sleeping for $1 seconds..."
    sleep "$1"
}

tap() {
    tap_pos="$1"
    tap_desc="$2"
    if [ -z "$1" ] || [ "$1" == "TODO" ] ; then
        echo "Oops, tap position for '$tap_desc' is '$tap_pos'" >&2
        exit 1
    fi
    ekko "Tapping at $tap_pos ($tap_desc)"
    if [ "$do_taps" != "0" ] ; then
        adb shell input tap $tap_pos
    fi
}

tap_finish() {
    # The finish button sometimes is in different places,
    # so click them all!
    tap "$finish_pos" "Finish"
    tap "$finish_pos2" "Finish (alternate position)"
}

if [ -n "$log_dir" ] ; then
    mkdir -p "$log_dir"
    
    log_file="$log_dir/workout-$(date +"%y%m%dT%H%M").log"
    echo "Redirecting stdout to ${log_file}" >&2
    exec >>"$log_file"
fi

if [ "$do_start_dmesg" != "0" ] ; then
    echo "Starting dmesg" >&2
    adb shell "su -c 'dmesg -w'" | sed 's/\r//' &
    sleep 1
fi

if [ "$do_start_logcat" != "0" ] ; then
    echo "Starting logcat" >&2
    adb logcat | sed 's/\r//' &
    sleep 1
fi


hour="$(date +'%H')"
if [ $hour -lt 12 ] ; then
    segment_of_day=morning
elif [ $hour -lt 17 ] ; then
    segment_of_day=afternoon
else
    segment_of_day=evening
fi
ekko "Good ${segment_of_day}!  It is $(date --iso-8601=second)"

for md in "${metadata[@]}" ; do
    ekko "test-run-metadata/$md"
done

# Launch the ifit app
tap "$launch_ifit_pos" "Launch iFit button"
sleap 30

if [ "$do_login" -ne 0 ] ; then
    if [ "$screen_size" != 16 ] ; then
        ekko "Oh no, automatic login only supported for 16-inch screen; aborting" 2>&1
        exit 1
    fi
    
    # Click Get Started
    ekko Click Get Started
    adb shell input tap 693 514
    sleap 5
    
    ekko Click Login
    adb shell input tap 602 582
    sleap 3
    
    ekko Click the email box
    adb shell input tap 583 375
    sleap 3
    
    ekko Enter login information
    adb shell input tap 738 505
    adb shell input tap 549 412
    adb shell input tap 194 690
    adb shell input tap 924 413
    adb shell input tap 493 504
    adb shell input tap 924 413
    adb shell input tap 549 412
    adb shell input tap 1308 685
    adb shell input tap 1275 505
    sleap 2
    adb shell input tap 65 596
    adb shell input tap 924 413
    adb shell input tap 444 598
    adb shell input tap 1044 417
    adb shell input tap 813 598
    adb shell input tap 68 683
    adb shell input tap 195 415
    adb shell input tap 1180 419
    adb shell input tap 64 413
    adb shell input tap 1046 409
    adb shell input tap 1275 505
    sleap 3
    
    ekko Accept the profile
    adb shell input swipe 500 500 300 300
    sleap 3
    adb shell input tap 674 674
    sleap 10
    
    # ekko Done for now
    # exit 0
fi

while [ $completed_workout_iterations -lt $max_workout_iterations ]; do
    ekko Starting workout $completed_workout_iterations of $max_workout_iterations...

    tap "$open_main_ifit_workout_pos" "Open main iFit workout"
    sleap 20

    tap "$continue_series_pos" "Continue series"
    sleep 1
    tap "$start_ifit_workout_pos" "Start workout"
    sleap 10

    tap "$accept_danger_agreement_pos" "Accept the danger"
    # sleap 60
    sleap 15

    tap "$skip_warmup_pos" "Skip warmup"

    ekko Let workout run for a while
    sleap $workout_duration

    if [ "$do_cooldown" != 0 ] ; then
        ekko Expose the start cooldown button
        tap "$arm_cooldown_pos" "Arm cooldown"
        sleap 2
        
        tap "$start_cooldown_pos" "Start cooldown"
        sleap 15
        
        tap "$end_cooldown_pos" "End cooldown"
        sleap 10

        tap_finish
    else
        tap "$pause_workout_pos" "Pause workout"

        sleap 4

        tap "$stop_workout_pos" "Stop workout"

        if [ "$do_accept_unsaved" != "0" ] ; then
            sleap 3 # Don't wait too long or we'll accidentally start a new workout!
            
            tap "$not_saved_okay_pos" "Workout not saved?  That's fine"
        fi

        sleap 10

        tap_finish
    fi

    sleap 15

    # Increment the loop
    let completed_workout_iterations=completed_workout_iterations+1
    
    ekko "Completed ${completed_workout_iterations} workouts"
done

ekko "Done"
