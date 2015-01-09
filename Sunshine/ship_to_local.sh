#!/bin/bash

if [ $# -ne 1 ]; then
  echo "Usage: ./ship_to.sh CHANNEL_ID"
  exit
fi

./clean_all.sh
rm -fr ship/*

python gen_build_settings.py $1

echo $1 > ./assets/channelId

ant release

if [ ! -d "ship" ]; then
  mkdir ship
fi

cp bin/Sunshine-release.apk ship/Sunshine-$1.apk


