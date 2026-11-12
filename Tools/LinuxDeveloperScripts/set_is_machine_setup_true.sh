#!/bin/bash

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_IS_MACHINE_SETUP_BOOL --es type boolean --es value true
