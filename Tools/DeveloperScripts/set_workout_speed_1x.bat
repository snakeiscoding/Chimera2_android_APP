@echo off

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key ACTION_SET_WORKOUT_SPEED_MULTIPLIER_FLOAT --es type float --es value 1.0