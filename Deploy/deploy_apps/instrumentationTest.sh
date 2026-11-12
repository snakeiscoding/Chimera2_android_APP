#!/usr/bin/env bash
m_consoles=""

getConsoles() {
    consoles=""
    while IFS="" read -r console || [ -n "$console" ]
    do
        consoles="$consoles$console "
    done < consoles_updated.txt
    m_consoles=$consoles
}

runTest() {

	echo "Wait for console to be ready."
	sh consoleReady.sh $1

    echo "Clear log and launch android launcher"
    timeout 2s adb logcat -c
    adb shell am start -n com.android.launcher3/com.android.launcher3.Launcher
    timeout 2s adb logcat -c
    adb shell am instrument -w -e annotation $2 $3
    retVal=$?
    timeout 2s adb logcat -d JHTTestTiming:d *:s
    adb shell touch /sdcard/available_for_test
    adb disconnect
    return retVal
}


if [ $# -gt 2 ]; then
    result=$(sh lockDeviceForTesting.sh $1 Any Any)
      echo $result
      if echo $result | grep -q "Device ready for test"; then
      	  runTest $1 $2 $3
          exit
      fi
else
    adb disconnect
    getConsoles
    while :; do
      for ip in ${m_consoles}; do
          echo "lock device $ip"
          result=$(sh lockDeviceForTesting.sh $ip Any Any)
          echo $result
          if echo $result | grep -q "Device ready for test"; then
              echo "Running test $1 $2 on $ip"
              runTest $ip $1 $2
              exit $?
          fi
      done
      sleep 60
    done
fi