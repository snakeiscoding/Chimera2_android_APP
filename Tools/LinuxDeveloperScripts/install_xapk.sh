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
            filename=$(basename -- "$xapk")
            unzipPath="$2/${filename%.*}"
            mkdir $unzipPath
            if command -v unzip &> /dev/null; then
              unzip $xapk -d $unzipPath
            else
              busybox unzip $xapk -d $unzipPath
            fi

            # install obb file if Android folder exists
            if [ -d $unzipPath/Android ];
            then
               echo "Install obb folder ./$filename/Android /sdcard/Android/"
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

            rm -rf $unzipPath

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
