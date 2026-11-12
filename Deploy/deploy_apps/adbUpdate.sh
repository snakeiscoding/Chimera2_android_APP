#!/usr/bin/env bash
sh prepareUpdate.sh $2
sh pushUpdate.sh $1
adb shell am broadcast -a com.jht.automatedupdate --es manifest /sdcard/update/automated/update.xml

