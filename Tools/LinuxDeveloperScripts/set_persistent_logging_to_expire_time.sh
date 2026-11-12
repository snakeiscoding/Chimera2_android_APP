#!/bin/bash
adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG --es type long --es value 1635804953
