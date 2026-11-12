#!/bin/bash
adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key ACTION_AUDIO_SELECT_PLUS_ADD_MEDIA_SOURCE_ENUM --es type integer --es value 1
