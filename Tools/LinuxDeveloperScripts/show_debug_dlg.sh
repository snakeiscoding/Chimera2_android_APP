#!/bin/bash

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key ACTION_SHOW_HAL_DLG --es type event --es value 0
