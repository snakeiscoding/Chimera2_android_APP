#!/bin/sh
# NOTE: This files needs to save new lines as UNIX (\n) and not Windows (\r\n) new line characters
# Exit code
# 11 - error during remount
# 12 - error durring copy
# 13 - error during delete
# 14 - error during apk install
# 15 - error during chmod
# $1 = JHT_FILES_PATH
# $2 = JHT_INSTALL_PATH

# ------------------------------------------------------------------------------
echo "Starting..."
ERROR_REMOUNT=11
ERROR_COPY=12
ERROR_DELETE=13
ERROR_APK_INSTALL=14
ERROR_CHMOD=15
echo "JHT_FILE_PATH=$1"
echo "JHT_INSTALL_PATH=$2"

# If $2 is nothing, avoid copying the entire file system.
# Default to the current folder.
if [ -z "$2" ]
then
	SRC_PATH="."
else
	SRC_PATH="$2/."
fi
echo "SRC_PATH=$SRC_PATH"

### INSTALL ANY FILE THAT GOES INTO /sdcard/jht
mkdir /sdcard/jht

# Should we ensure a clean install? Is there anything other than assets here?
#rm -rf /sdcard/jht/*

echo "==> Copying assets"
cp -rfvp $SRC_PATH /sdcard/jht/
if [ "$?" -ne 0 ]; then exit $ERROR_COPY; fi

# remove update script? alternative would to iterate through all subfolders?
rm -f /sdcard/jht/update.sh

echo 
echo "Done!!!!!"
exit 0
