#!/usr/bin/env bash
if [ $# -lt 1 ]; then
    printf "USAGE: $0 patch.tar.gz single|master|slave\n"
    exit 1
fi
case $1 in
    -h | -help | --h | --help)
        printf "USAGE: $0 patch.tar.gz single|master|slave\n"
        exit 1
    ;;
    *)
    ;;
esac
if [ ! -f $1 ]; then
    printf "$1 does not exit or empty...\n"
    printf "USAGE: $0 patch.tar.gz single|master|slave\n"
    exit 1
fi
dir=$(cd `dirname $0`/..;pwd)
patch="`basename $1 .tar.gz`"
if [ "`dirname $1`" != "$dir" ];then
    cp -f $1 $dir/
fi
tar -zxpf $dir/`basename $1` -C $dir/
if [ -d $dir/$patch/lib ];then
    printf "update third part jar\n"
    cp -rf $dir/$patch/lib/* $dir/lib
fi
if [ "$2" == 'master' -o "$2" == 'single' -a -d $dir/$patch/web ];then
    printf "update cii_da web\n"
    cp $dir/web/config.js $dir/
    rm -rf $dir/web
    cp -rf $dir/$patch/web $dir/
    mv -f $dir/config.js $dir/web/
fi
if [ -f $dir/$patch/cii_da.jar ];then
    printf "update cii_da jar\n"
    cp -f $dir/$patch/cii_da.jar $dir/lib
    sleep 1
    printf "restart cii_da server\n"
    $dir/bin/cii-stop.sh
    $dir/bin/cii-start.sh daemon
fi
printf "clear patch file\n"
rm -rf $dir/`basename $1`
rm -rf $dir/$patch
echo "`date +%Y-%m-%d,%H:%M:%S`=>$patch" >> $dir/update.log
