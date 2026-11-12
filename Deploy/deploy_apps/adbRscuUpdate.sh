#!/usr/bin/env bash
echo "RSCU Update $1 $2"
# sh prepareUpdate.sh $2
RSCU_VERSION="`cat rscu_version.txt`"
RSCU_LOCATION="`cat $1.rscu_location.txt`"
RSCU_MD5="`cat $1.rscu_md5.txt`"
echo "adb shell am broadcast -a com.jht.automatedupdate --ez rscu true --es rscu_version \"$RSCU_VERSION\" --es rscu_location \"$RSCU_LOCATION\" --es rscu_md5 \"$RSCU_MD5\""
adb shell am broadcast -a com.jht.automatedupdate --ez rscu true --es rscu_version "$RSCU_VERSION" --es rscu_location "$RSCU_LOCATION" --es rscu_md5 "$RSCU_MD5"

