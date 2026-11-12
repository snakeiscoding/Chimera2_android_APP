#!/bin/bash
adb shell am broadcast -a "com.jht.loggerservice.ACTION_LOG_DATA_MATRIX_KEY" --es key DATA_AUDIO_ACTIVE_INPUT_SOURCE_ENUM --es type integer --es value none
