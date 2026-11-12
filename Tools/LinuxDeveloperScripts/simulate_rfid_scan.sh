#!/bin/bash

rfStandards=("0" "1" "2" "3" "4" "5" "255")
invalidRfStandard=1

while [ "$invalidRfStandard" -eq 1 ]
do
    echo "Please Choose a RF Standard:"
    echo "0. LegicRf"
    echo "1. Iso15693"
    echo "2. Iso14443A"
    echo "3. Iso14443B"
    echo "4. InsideSecure"
    echo "5. SonyFelica"
    echo "255. Unknown"
    read -p "> " rfStandard

    for x in ${rfStandards[@]}; do
        if [ "$rfStandard" = "$x" ]; then
            invalidRfStandard=0
        fi
    done
done

echo ""
read -p "RFID UID (Hex): " rfUid

adb shell am broadcast -a "com.jht.rfid_sim" --es rfid_scan "$rfStandard,$rfUid"