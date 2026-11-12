#!/bin/bash

adb shell settings put secure jht_disable_persistent 0
sleep 5
adb reboot
