@echo off

adb shell am broadcast -a com.jht.loggerservice.ACTION_CONSOLE_REBOOT --es reason rebootByScript