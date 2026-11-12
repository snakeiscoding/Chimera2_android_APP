#!/bin/bash

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_SAFETY_KEY_STATUS_INT --es type integer --es value 16
