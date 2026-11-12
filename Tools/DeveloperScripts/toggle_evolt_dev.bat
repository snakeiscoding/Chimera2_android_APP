:restart
@echo off
setlocal enabledelayedexpansion
echo 0 - Toggle SMM, Age and HR [Guest Account]
echo 1 - Toggle Age
echo 2 - Toggle HR
echo 3 - Toggle SMM
echo 4 - Default Values SMM - 22.7, Age - 22, HR - 80
set /p "data=Please Choose from above: "

if %data% == 0 (
	set /p "smm=Enter SMM Value: "
	set /p "age=Enter age of the user: " 
	set /p "hr=Enter Heart Rate: " 

	adb  shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_USER_EVOLT_SMM_FLOAT --es type float --es value !smm! && adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_USER_AGE_INT --es type integer --es value !age! && adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_HEART_RATE_INT --es type integer --es value !hr!

) else if %data% == 1 (
	set /p "age=Enter age of the user: "

	adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY"--es key DATA_USER_AGE_INT --es type integer --es value !age!

) else if %data% == 2 (

	set /p "hr=Enter Heart Rate: " 

	adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_HEART_RATE_INT --es type integer --es value !hr!

) else if %data% == 3 (

	set /p "smm=Enter SMM Value: "

	adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_USER_EVOLT_SMM_FLOAT --es type float --es value !smm!

) else if %data% == 4 (

	adb  shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_USER_EVOLT_SMM_FLOAT --es type float --es value 22.7 && adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_USER_AGE_INT --es type integer --es value 22 && adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key DATA_HEART_RATE_INT --es type integer --es value 80

) else (

	echo Please use valid option
	goto restart

)