@echo off

set rfStandards=0 1 2 3 4 5 255
goto :getRfStandard

:getRfStandard
    echo Please Choose a RF Standard:
    echo 0. LegicRf
    echo 1. Iso15693
    echo 2. Iso14443A
    echo 3. Iso14443B
    echo 4. InsideSecure
    echo 5. SonyFelica
    echo 255. Unknown
    set /p rfStandard="> "
    for %%x in (%rfStandards%) do (
        if %%x==%rfStandard% goto :getUID
    )
    goto :getRfStandard

:getUID
    set /p rfUID="RFID UID (Hex): "
    goto :broadcast

:broadcast
    adb shell am broadcast -a "com.jht.rfid_sim" --es rfid_scan %rfStandard%","%rfUID%
    exit