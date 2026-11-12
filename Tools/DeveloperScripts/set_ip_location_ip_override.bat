@echo off

set quoted_value='%1'
if %quoted_value% == '""' set quoted_value=''
if %quoted_value% == 'none' set quoted_value=''
if %quoted_value% == 'RIPE' set quoted_value=81.1.1.1
if %quoted_value% == 'ARIN' set quoted_value=76.1.1.1
if %quoted_value% == 'AFRINIC' set quoted_value=105.1.1.1
if %quoted_value% == 'APNIC' set quoted_value=110.1.1.1
if %quoted_value% == 'MX' set quoted_value=8.243.208.0
if %quoted_value% == 'CA' set quoted_value=51.79.121.4
if %quoted_value% == 'US' set quoted_value=66.139.132.0

echo Setting IP override to %quoted_value%

adb shell "am broadcast -a com.jht.loggerservice.ACTION_UPDATE_DATA_MATRIX_KEY --es key DATA_IP_LOCATION_IP_OVERRIDE_STRING --es type string --es value %quoted_value%"
