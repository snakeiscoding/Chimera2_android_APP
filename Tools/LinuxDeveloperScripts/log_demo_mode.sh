#!/bin/bash

adb shell am broadcast -a "com.jht.loggerservice.ACTION_LOG_DATA_MATRIX_KEY" --es key DATA_IS_DEMO_MODE_BOOL --es type boolean --es value not_needed
