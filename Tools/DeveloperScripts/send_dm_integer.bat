@echo off

if "%~2"=="" goto you_did_it_wrong

adb shell am broadcast -a "com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY" --es key "%~1" --es type integer --es value "%~2"
goto eof

:you_did_it_wrong
echo %~nx0: Publish an integer to DataMatrix.
echo:
echo Usage: %~nx0 (some key) (some integer value) >&2
echo:
echo Example: %~nx0 DATA_SCREEN_TIMEOUT_SEC_INT 42
exit /B 1

:eof
