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

echo "JHT_FILE_PATH=$1"
echo "JHT_INSTALL_PATH=$2"

echo 
echo "Done!!!!!"
exit 0
