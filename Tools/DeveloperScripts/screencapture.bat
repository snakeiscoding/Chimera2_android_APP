@echo off

rem Usage:
rem   screencapture.bat -o some-directory/ ; save to a new file in the specified directory
rem   screencapture.bat -o some-file.png   ; save to a specific file
rem   screencapture.bat                    ; save to a new file in the current directory
rem
rem Note that it is also possible to use 'adb screencap' and send the output
rem directly to standard output so that no temporary file is needed.
rem This is only troublesome because adb on Windows insists on
rem inserting carriage-return characters before every newline character,
rem so you must either use some non-binary encoding
rem (e.g. base64) or remove the inappropriate carriage-returns
rem (fortunately, the PNG header makes it simple to detect them).
rem Piping through `sed "s/\r\n/\n/g"` seems to do the job.
rem
rem e.g. `adb shell "screencap -p | base64" | base64 -di >my-screenshot.png`

setlocal

rem configurable parameters
set screencap_file_prefix=screenshot

rem the dir on the device where the screenshot will be stored temporarily
set screencap_remote_dir=/sdcard/

rem adb path, leave blank if adb is already on the user or system path
if "%screencap_adb_exe%"=="" set screencap_adb_exe=adb.exe

if "%~1"=="-o" set screencap_dest=%~2
if "%screencap_dest%"=="" set screencap_dest=.\
set screencap_dest=%screencap_dest:/=\%

rem PowerShell's Get-Date might be the only way to get the date/time in a specific format
rem (%DATE% and `date /t` might return it different depending on locale)
for /f %%a in ('powershell -Command "Get-Date -format yyyyMMdd-HHmmss"') do set screencap_datetime=%%a

set screencap_file_name=%screencap_file_prefix%-%screencap_datetime%.png

rem create output file from parameters and date and time
set screencap_remote_file=%screencap_remote_dir%%screencap_file_name%
if "%screencap_dest:~-1%"=="\" (
  set screencap_dest_file=%screencap_dest%%screencap_file_name%
) else (
  set screencap_dest_file=%screencap_dest%
)

rem do it!
echo Taking Screenshot at %screencap_datetime%
echo Output File: %screencap_dest_file%
echo.
%screencap_adb_exe% shell screencap -p %screencap_remote_file%
%screencap_adb_exe% pull "%screencap_remote_file%" "%screencap_dest_file%"
%screencap_adb_exe% shell rm %screencap_remote_file%
echo.
pause
