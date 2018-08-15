#! /bin/sh
PIDS=`ps ax | grep 'com.unimas.RestServer' | grep java | grep -v grep | awk '{print $1}'`
if [ ! -z "$PIDS" ]; then
for pid in $PIDS
do
  kill -9 $pid
done
fi

