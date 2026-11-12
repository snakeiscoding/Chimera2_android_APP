@echo off
echo this script will output the machine id to a text file in the same location you run this

adb logcat -c && adb shell am broadcast -a "com.jht.loggerservice.ACTION_LOG_DATA_MATRIX_KEY" --es key DATA_DAPI_MACHINE_UUID_STRING --es type string --es value none && adb logcat LoggerService:W *:S -d > get_machine_id.txt