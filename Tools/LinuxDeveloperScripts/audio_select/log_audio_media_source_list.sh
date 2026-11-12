#!/bin/bash
adb shell am broadcast -a "com.jht.loggerservice.ACTION_LOG_DATA_MATRIX_KEY" --es key DATA_AUDIO_INPUT_SOURCE_ENUM_STRING_LIST --es type string_list --es value none
