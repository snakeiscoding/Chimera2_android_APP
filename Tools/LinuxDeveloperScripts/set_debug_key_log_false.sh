#!/bin/bash

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DM_DEBUG_TRACING_ON --es type boolean --es value false
