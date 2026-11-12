#!/usr/bin/env bash
m_consoles=""
check_adb_connection() {
    if timeout 5s adb devices $1 | grep -q "$1"; then
        echo "already connected $1"
        return 0
    fi
    echo "Going to connect to $1"
    if timeout 10s adb connect $1 | grep -q "connected to"; then
        echo "connected $1"
        sleep 2
        return 0
    fi
    echo "could not connect to $1"
    return 1
}

inUpdate() {
    if echo $1 | grep -qe "START u0 .*com.jht.updateservice/\.BackgroundActivity"; then
	    if echo $1 | grep -q "Force finishing activity com.jht.updateservice/.BackgroundActivity"; then
    		return 1
    	else
    		return 0
    	fi
    fi
    return 1
}

consoleReady() {
    # Make an ADB connection.
    if check_adb_connection $1; then
        # Connected to the console.  See if we are in an update or idle.
        echo "get logcat $1"
        result=$(timeout 2s adb logcat -d)
        if inUpdate $result; then
            return 1
        fi
        if echo $result | grep -q "START u0 .*WelcomeActivity"; then
            return 0
        elif echo $result | grep -q "START u0 .*GoScreenActivity"; then
            return 0
        elif echo $result | grep -q "START u0 .*EGymActivity"; then
            return 0
        fi
    fi
    return 1
}


updateConsoles() {
    consoles=""
    for console in ${m_consoles}; do
        adb disconnect
        echo "update console $console"
        if consoleReady $console; then
            echo "2 update console $console"
            adb shell rm -f /sdcard/testing
            adb shell touch /sdcard/available_for_test
            sh adbRscuUpdate.sh update.xml
            consoles="$consoles$console "
        fi
    done
    m_consoles=$consoles
}

waitForConsolesToUpdate() {
    consoles=""
    for console in ${m_consoles}; do
        adb disconnect
        timeout 1200s sh consoleReady.sh $console
        result="$?"
        if [ "$result" -eq 0 ]; then
            consoles="$consoles$console "
        fi

    done
    adb disconnect
    m_consoles=$consoles
}

updateConsoleList() {
    consoles=""
    for console in ${m_consoles}; do
        consoles="$consoles$console\n"
    done
    echo $consoles > consoles_updated.txt
}

m_consoles=$(java -jar ConsoleFinder.jar chimera_main_matrix_chimera)
echo $m_consoles
updateConsoles
sleep 30
waitForConsolesToUpdate
updateConsoleList
if [ -z $m_consoles ]; then
    exit 1
fi
exit 0


