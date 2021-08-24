#!/bin/bash

aosp_path=/home/android/driver/commit-c330/LA.UM.7.6.2/LINUX/android
libpath=$aosp_path/out/host/linux-x86/lib64/
signapk_jar=$aosp_path/out/host/linux-x86/framework/signapk.jar

src=../build/outputs/apk/debug/app-debug.apk
dest=./apk-sign.apk
keypath=./tinno-keys

java -Djava.library.path=$libpath -jar $signapk_jar $keypath/platform.x509.pem $keypath/platform.pk8 $src $dest

#install
adb install -r $dest


