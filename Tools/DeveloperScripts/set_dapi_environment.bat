@echo off
echo MUST REACH THE SPLASH SCREEN FOR THIS CHANGE TO TAKE EFFECT
echo I recommend hitting guest and then the home button - this is the quickest way
echo 0 - Dev
echo 1 - QA
echo 2 - Staging
echo 3 - Prod
set /p "id=Enter DAPI code: "

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_DAPI_ENVIRONMENT_ENUM_INT --es type integer --es value %id%