@echo off

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_MARKETING_WELCOME_SCREEN_ADS_BOOL --es type boolean --es value true
adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_MARKETING_FEATURE_CAROUSEL_ADS_BOOL --es type boolean --es value true
