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



### INSTALL USER APPS

echo 
echo "Checking if any user APKs exists..."
ls $2/*.apk
if [ "$?" -eq 0 ]; then
	for file in $2/*.apk
	do
		count=1 # try 3 times
		while [ $count -lt 4 ]
		do
        echo "==> Installing apk: $file"
        pm install -r -d $file | grep Success
        if [ "$?" -eq 0 ]; 
        then 
          count=4
        else
          echo "==> Error: try #:$count"
          count=$((count+1))
          if [ $count -eq 4 ]; then exit $ERROR_APK_INSTALL; fi
        fi
		done
	done
fi


# Run any custom scripts 
if [ -f $2/misc/custom.sh ];
then
   echo "Running custom script"
   sh $2/misc/custom.sh $2
fi

# Run AOT compilation on chimera package
cmd package compile -m speed -f com.jht.chimera

echo 
echo "Done!!!!!"
exit 0
