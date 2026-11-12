#!/bin/bash

adb shell settings put secure jht_disable_persistent 1
sleep 5
adb reboot
