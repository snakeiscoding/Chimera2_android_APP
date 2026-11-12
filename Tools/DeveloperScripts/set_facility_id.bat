@echo off
echo MUST REACH THE SPLASH SCREEN FOR THIS CHANGE TO TAKE EFFECT
echo I recommend hitting guest and then the home button - this is the quickest way
set /p "id=Enter Facility ID: "

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_CLUB_ID_INT --es type integer --es value %id%