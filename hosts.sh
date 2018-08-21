#!/usr/bin/env bash
dir=`dirname $0`
if [ -f $dir/hosts ];then
    echo "" >> /etc/hosts
    while read l;do
        echo "$l" >> /etc/hosts
    done < $dir/hosts
    rm -f $dir/hosts
fi


