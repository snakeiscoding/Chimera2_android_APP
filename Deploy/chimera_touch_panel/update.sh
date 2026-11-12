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
P1_32a="4"
P1_32b="6"
robootpanelid=$(getprop "ro.boot.panel_id")

if [ $robootpanelid = $P1_32a ] || [ $robootpanelid = $P1_32b ]; then
  echo "Installing Touch Panel Firmware on 32\" Chimera"
     pm install -r -d $2/eGalaxUpdateAuto_v0.52_Orion*.apk | grep Success
     if [ "$?" -eq 0 ]; then
          am start -n com.eeti.android.egalaxupdateauto/com.eeti.android.egalaxupdateauto.eGalaxUpdateAuto
     fi
elif wm size | grep -q '1366'; then
   echo "Installing Touch Panel Firmware on 16\" Chimera"
   pm install -r -d $2/eGalaxUpdateAuto_v0.41_PCAP3188UI_0623_v06.apk | grep Success
   if [ "$?" -eq 0 ]; then
        am start -n com.eeti.android.egalaxupdateauto/com.eeti.android.egalaxupdateauto.eGalaxUpdateAuto
   fi
else
   echo "Installing Touch Panel Firmware on 22\" Chimera"
   pm install -r -d $2/eGalaxUpdateAuto_v0.40_PCAP3189UR_2484_v00_T4.apk | grep Success
   if [ "$?" -eq 0 ]; then
        am start -n com.eeti.android.egalaxupdateauto/com.eeti.android.egalaxupdateauto.eGalaxUpdateAuto
   fi
fi
sleep 60

echo
echo "Done!!!!!"
exit 0
