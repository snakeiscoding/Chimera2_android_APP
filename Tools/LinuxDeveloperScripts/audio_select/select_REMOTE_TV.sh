#!/bin/bash
adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT --es type integer --es value 3
