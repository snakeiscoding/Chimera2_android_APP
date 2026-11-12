#!/bin/sh
# NOTE: This files needs to save new lines as UNIX (\n) and not Windows (\r\n) new line characters
# Exit code.
# 11 - error during remount
# 12 - error durring copy
# 13 - error during delete
# 14 - error during apk install
# 15 - error during chmod
# 16 - error verifying version number
# 17 - error verifying md5 of file
# $1 = JHT_FILES_PATH
# $2 = JHT_INSTALL_PATH

self_name="$0"

# ------------------------------------------------------------------------------
long_log_prefix="$self_name ($(md5sum -b "$0")): "
echo "${long_log_prefix}Starting at $(date +"%Y%m%dT%H%M%S")..."
ERROR_REMOUNT=11
ERROR_COPY=12
ERROR_DELETE=13
ERROR_APK_INSTALL=14
ERROR_CHMOD=15
ERROR_HASH=17
ERROR_VERSION=18

# contains(string, substring)
#
# Returns 0 if the specified string contains the specified substring,
# otherwise returns 1.
#
# Adapted from https://stackoverflow.com/questions/2829613/how-do-you-tell-if-a-string-contains-another-string-in-posix-sh !
contains() {
    haystack="$1"
    needle="$2"
    # Q: What the heck is '#*'?
    # A: Seems to return the part of haystack *after* needle,
    #    or the entire haystack if needle's not found, hence:
    test "${haystack#*$needle}" != "$haystack"
}

# packages to clean and AOT (apk file name must contain package name to be detected)
packagesToClean=(
    "com.jht.solitaire"
    "com.ifit.standalone"
)
# if file name contains a package name from the list return it, otherwise empty
packageNeedsClean() {
    local fileName=$1
    for package in "${packagesToClean[@]}"; do
        if contains "$fileName" "$package" ; then
            echo "$package"
            break
        fi
    done
}

echo "JHT_FILE_PATH=$1"
echo "JHT_INSTALL_PATH=$2"
echo 
echo "Checking if any user APKs exists..."
ls $2/*.apk
if [ "$?" -eq 0 ]; then
    for file in $2/*.apk
    do
        install_attempts_remaining=3
        needClean=$( packageNeedsClean "$file" )
        echo "needClean is $needClean"
        if [ "$needClean" != "" ] && [[ $(pm list packages $needClean | grep $needClean) = *$needClean* ]] ; then
            echo "==> Uninstall package $needClean $file"
            #pm disable "$needClean"
            pm uninstall "$needClean"
        fi
        # ifit block logic
        if contains "$file" "com.ifit.standalone" ; then
            if [ -f /mnt/backup/block_ifit ] ; then
                message="iFit is blocked; skipping $file"
                install_attempts_remaining=0
            else
                message="iFit is *not* blocked; continuing with installation"
            fi
            echo "$message"
            echo "${long_log_prefix}$message" > "$2/ifit-install.log"
        fi
        
        # install package
        while [ $install_attempts_remaining -gt 0 ]
        do
            echo "==> Installing apk: $file"
            pm install -r -d "$file" | grep Success
            if [ "$?" -eq 0 ]; 
            then 
                install_attempts_remaining=0
            else
                echo "==> Error: attempts remaining #:$install_attempts_remaining"
                install_attempts_remaining=$((install_attempts_remaining-1))
                if [ $install_attempts_remaining -eq 0 ]; then exit $ERROR_APK_INSTALL; fi
            fi
        done

        # AOT check - do not fail script if AOT fails
        if [ "$needClean" != "" ] ; then
            echo "==> AOT package $needClean $file"
            cmd package compile -m speed -f "$needClean" || true
        fi
    done
fi


echo
echo "Checking if any user XAPKs exists..."
ls -d $2/*.xapk
if [ "$?" -eq 0 ]; then
    for xapk in $2/*.xapk
    do
        count=1 # try 3 times
        while [ $count -lt 4 ]
        do
            echo "==> Installing xapk: xapk"
            echo "pm install-create -r -d"
            # Check if xapk is already extracted or needs to be unzipped
            if [ -d $xapk ]
            then
                unzipPath=$xapk
            else
                filename=$(basename -- "$xapk")
                unzipPath="$2/${filename%.*}"
                mkdir $unzipPath
                # PLATFORM=$(getprop ro.product.name)
                if command -v unzip &> /dev/null; then
                  unzip $xapk -d $unzipPath
                else
                  busybox unzip $xapk -d $unzipPath
                fi
            fi

            # install obb file if Android folder exists
            if [ -d $unzipPath/Android ];
            then
               echo "Install obb folder $unzipPath/Android /sdcard/Android/"
               cp -rf $unzipPath/Android /sdcard/
            fi

            # create session
            SESSION_ID=$(pm install-create  -r -d | cut -d "[" -f2 | cut -d "]" -f1)
            echo "SESSION_ID = $SESSION_ID"

            echo "dir = $unzipPath"
            INDEX=0
            for file in $unzipPath/*.apk
            do
                # add session apk
                echo "pm install-write $SESSION_ID $INDEX $file"
                pm install-write $SESSION_ID $INDEX $file
                INDEX=$((INDEX+1))
            done

            # end session
            echo "pm install-commit $SESSION_ID"
            pm install-commit $SESSION_ID

            if [ "$?" -eq 0 ];
            then
              rm -rf $unzipPath
              count=4
            else
              rm -rf $unzipPath
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


echo 
echo "$self_name: Done!!!!!"
exit 0
