#!/usr/bin/env bash
adb shell mkdir -p /sdcard/update/automated
adb shell rm -rf /sdcard/update/automated/*
for file in *.zip
do
	adb push $file /sdcard/update/automated/$file
done
adb push $2 /sdcard/update/automated
sleep 2
