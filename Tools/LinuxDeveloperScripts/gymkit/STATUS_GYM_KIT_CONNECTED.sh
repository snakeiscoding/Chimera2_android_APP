#!/bin/bash

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_GYM_KIT_STATUS_INT --es type integer --es value 1
adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_GYM_KIT_CALORIES_ACTIVE_INT --es type integer --es value 10
adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_GYM_KIT_CALORIES_TOTAL_INT --es type integer --es value 20
adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key ACTION_UPDATE_GYM_KIT_HEART_RATE_INT --es type integer --es value 80
adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_GYM_KIT_AVE_HEART_RATE_INT --es type integer --es value 78
