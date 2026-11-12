#!/usr/bin/env bash
runIfitTest() {

	echo "Wait for update to complete"
	sh consoleReady.sh $1

    echo "Clear log and launch android launcher"
    timeout 2s adb logcat -c
    adb shell am start -n com.android.launcher3/com.android.launcher3.Launcher
    timeout 2s adb logcat -c

    echo "{\"getStartedDelaySeconds\": $GetStartedDelaySeconds, \"guestWorkoutSelectionSeconds\": $GuestWorkoutSelectionSeconds, \"workoutTimeSeconds\": $WorkoutTimeSeconds, \"endWarmupTimeSeconds\": $EndWarmupTimeSeconds, \"workoutSelectionDelaySeconds\": $WorkoutSelectionDelaySeconds, \"acceptDelaySeconds\": $AcceptDelaySeconds, \"startWorkoutDelaySeconds\": $StartWorkoutDelaySeconds }" > ifitTestSettings.json
    adb shell mkdir -p /sdcard/test
    adb push ifitTestSettings.json /sdcard/test/ifitTestSettings.json
    for i in `seq 1 $TestRuns`;
    do
        echo "Running test $i"
        timeout 2s adb logcat -c
        adb shell am instrument -w -e class "com.jht.chimera.ui.ifit.IFitWorkoutTest#testIFitWorkout" com.jht.chimera.test/.runner.SplashAppTestRunner
        timeout 2s adb logcat -d JHTTestTiming:d *:s
    done
    adb shell rm -rf /sdcard/test/*
    adb shell touch /sdcard/available_for_test
    adb disconnect

}

if [ $# -gt 0 ]; then
    result=$(sh lockDeviceForTesting.sh $1 Any Any)
      echo $result
      if echo $result | grep -q "Device ready for test"; then
      	  runIfitTest $1
          exit
      fi
else
    while :; do
      for ip in ${chimeraIPAddress}; do
          echo $ip
          ipAddress="10.10.1.${ip}"
          echo $ipAddress
          result=$(sh lockDeviceForTesting.sh $ipAddress Any Any)
          echo $result
          if echo $result | grep -q "Device ready for test"; then
              runIfitTest $ipAddress
              exit
          fi
      done
      sleep 60
    done
fi