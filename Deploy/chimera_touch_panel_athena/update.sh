#!/bin/sh
# NOTE: This files needs to save new lines as UNIX (\n) and not Windows (\r\n) new line characters
# Exit code
# 11 - error during remount
# 12 - error durring copy
# 13 - error during delete
# 14 - error during apk install
# 15 - error during chmod
# 16 - error verifying version number
# 17 - error verifying md5 of file
# $1 = JHT_FILES_PATH
# $2 = JHT_INSTALL_PATH

# ------------------------------------------------------------------------------
echo "Starting..."
ERROR_REMOUNT=11
ERROR_COPY=12
ERROR_DELETE=13
ERROR_APK_INSTALL=14
ERROR_CHMOD=15
ERROR_HASH=17
ERROR_VERSION=18

   echo "Installing Touch Panel Firmware on Athena"
pm install -r -d $2/eGalaxUpdateAuto_v0.46_Sirius_2484_v00_T7.apk | grep Success
if [ "$?" -eq 0 ]; then
    am start -a com.eeti.android.egalaxupdateauto
    adb shell am start -n com.eeti.android.egalaxupdateauto/com.eeti.android.egalaxupdateauto.eGalaxUpdateAuto
fi
sleep 60
echo 
echo "Done!!!!!"
exit 0
