if test -f /data/data/com.amazon.kindle/shared_prefs/UserSettings.xml ; then
    # File exists. Most likely was downloaded via SSO or was previously logged in.  Turn on scrolling.
    if grep -q CONTINUOUS_SCROLL /data/data/com.amazon.kindle/shared_prefs/UserSettings.xml ; then
        # Setting exists.  Make sure it is what we want.
        sed -E 's/\"CONTINUOUS_SCROLL_REFLOWABLE(.*)false"/\"CONTINUOUS_SCROLL_REFLOWABLE\1true\"/g' /data/data/com.amazon.kindle/shared_prefs/UserSettings.xml
    else
        # Setting did not exist.  Add it.
        sed '/COVER_IMAGE_HEIGHT_SMALL.*/a \ \ \ \ <boolean name=\"CONTINUOUS_SCROLL_REFLOWABLE\" value=\"true\" \/>\n    <boolean name=\"CONTINUOUS_SCROLL_REFLOWABLE_EVER_ENABLED\" value=\"true\" \/>' /data/data/com.amazon.kindle/shared_prefs/UserSettings.xml
    fi
else
      # Settings file is empty.  Most likely this is the first open or guest.  Copy over the default
      # to make sure the settings are what we want them to be.
      mkdir -p /data/data/com.amazon.kindle/shared_prefs
      cp /sdcard/jht/kindle/* /data/data/com.amazon.kindle/shared_prefs
fi