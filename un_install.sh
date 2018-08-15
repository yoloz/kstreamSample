dir=$(cd `dirname $0`/..;pwd)

PIDS=`ps ax | grep 'com.unimas.RestServer' | grep java | grep -v grep | awk '{print $1}'`
if [ ! -z "$PIDS" ]; then
    for pid in $PIDS
    do
        kill -9 $pid
    done
fi
PIDS=$(ps ax | grep -i 'kafka.Kafka' | grep java | grep -v grep | awk '{print $1}')
if [ ! -z "$PIDS" ];then
    for pid in $PIDS
    do
        kill -9 $pid
    done
fi
PIDS=$(ps ax | grep -i 'quorum.QuorumPeerMain' | grep java | grep -v grep | awk '{print $1}')
if [ ! -z "$PIDS" ];then
    for pid in $PIDS
    do
        kill -9 $pid
    done
fi
PIDS=$(ps ax | grep -i 'logstash' | grep java | grep -v grep | awk '{print $1}')
if [ ! -z "$PIDS" ]; then
    for pid in $PIDS
    do
        kill -9 $pid
    done
fi

rm -rf $1
rm -rf $2
rm -rf $dir
