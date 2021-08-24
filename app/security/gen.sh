#!/bin/bash

keypath=./wiko_keys
name=platform_keystore.jks

# ./keytool-importkeypair -k [jks文件名] -p [jks的密码] -pk8 platform.pk8[私钥] -cert platform.x509.pem[公钥证书] -alias [jks的别名]

./keytool-importkeypair -k $name -p android -pk8 $keypath/platform.pk8 -cert $keypath/platform.x509.pem -alias platform
