#!/usr/bin/env bash
inUpdate() {
    if echo $result | grep -qe "START u0 .*com.jht.updateservice/\.BackgroundActivity"; then
	    if echo $result | grep -q "Force finishing activity com.jht.updateservice/.BackgroundActivity"; then
    		return 1
    	else
    		return 0
    	fi
    fi
    return 1
}

update_failed() {
    if echo $result | grep -qe "There was an error installing the software"; then
        return 0
    fi
    return 1
}

echo "console ready $1"
while [ true ]
do
	if ! test -z $1; then
        echo "check connection $1"
        sh checkADBConnection.sh $1
        sleep 3
    else
        echo "Wired connection assumed"
    fi
    result=$(timeout 2s adb logcat -d)
    echo "Check if in update $1"
    if inUpdate $result; then
        # We are in an update
        echo "In update $1"
        if update_failed $result; then
            echo "Update failed $1"
            adb disconnect
            exit 1
        fi
        adb disconnect
        sleep 60
    else
        # Make sure the welcome screen is shown
        if echo $result | grep -q "START u0 .*WelcomeActivity"; then
            echo "Welcome screen was launched $1"
            echo "$1 is ready"
            break
        elif echo $result | grep -q "START u0 .*GoScreenActivity"; then
            echo "Go screen was launched $1"
            echo "$1 is ready"
            break
        elif echo $result | grep -q "START u0 .*EGymActivity"; then
            echo "Go screen was launched $1"
            echo "$1 is ready"
            break
        else
            # Welcome screen is not up yet.  Sleep and check again.
            adb disconnect
            sleep 60
        fi
    fi
done

