#!/usr/bin/env bash
lock_device() {
    if timeout 1s adb shell rm /sdcard/available_for_test; then
        return 0
    fi
    echo "Not Available For Testing"
    adb disconnect $1
    return 1

}

is_machine() {
    if [ $2 = "Any" ]; then
        return 0
    fi
    timeout 2s adb shell su root grep -qe "DATA_FRAME_TYPE_STRING.*$2" /data/data/com.jht.chimera/shared_prefs/SystemSettings.xml

    return $?
}

is_screen() {
    if [ $2 = "Any" ]; then
        return 0
    fi
    timeout 5s adb shell wm size | grep -Fq $2
    return $?
}

check_adb_connection() {
    if timeout 5s adb devices $1 | grep -q "$1"; then
        return 0
    fi
    if timeout 5s adb connect $1 | grep -q "connected to"; then
        sleep 2
        return 0
    fi
    return 1
}

if check_adb_connection $1; then
    if lock_device $1; then
        if is_machine $1 $2; then
            if is_screen $1 $3; then
                echo "Device ready for test"
                exit 0
            else
                echo "Wrong screen size"
            fi
        else
            echo "Wrong machine type"
        fi
        adb shell touch /sdcard/available_for_test
        adb disconnect $1
    fi
else
    echo "Cannot connect to $1"
fi
exit 0
