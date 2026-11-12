#!/usr/bin/env bash
sh consoleReady.sh $1
for file in *.apk
do
	count=1 # try 3 times
	while [ $count -lt 4 ]
	do
		echo "==> Installing apk: $file"
		adb install -r -d $file | grep Success
		if [ "$?" -eq 0 ]; 
		then 
			count=4
		else
			echo "==> Error: try #:$count"
			count=$((count+1))
			if [ $count -eq 4 ]; then exit 14; fi
		fi
	done
done
adb reboot &
sleep 30
sh consoleReady.sh $1

