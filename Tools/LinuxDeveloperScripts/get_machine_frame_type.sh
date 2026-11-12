#!/bin/bash

adb logcat -c && adb shell am broadcast -a "com.jht.loggerservice.ACTION_LOG_DATA_MATRIX_KEY" --es key DATA_FRAME_TYPE_STRING --es type string --es value none && adb logcat LoggerService:W *:S -d > get_machine_type.txt
