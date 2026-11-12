#!/usr/bin/env bash
while [ true ]
do
    if [ $# -gt 0 ]; then
        if timeout 5s adb devices $1 | grep -q "$1"; then
            echo "device is connected $1"
        	break
        fi
		if timeout 5s adb connect $1 | grep -q "connected to"; then
            echo "2 device is connected $1"
		    break
		else
            echo "try again $1"
		    sleep 5
		fi
    else
        echo "Assuming ethernet"
        break
    fi
done
