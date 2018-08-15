#!/usr/bin/env bash
usage(){
    printf  "USAGE: $0 option\n"
    printf  "       -start 启动zookeeper和kafka\n"
    printf  "       -stop 停止zookeeper和kafka\n"
}
if [ $# -lt 1 ];then
    usage
    exit 1
fi
case $1 in
    -h |-help| --h | --help)
        usage
        exit 1
    ;;
    *)
    ;;
esac

dir=`cd $(dirname $0)/..;pwd`
if [ "$1" == '-start' ];then
    if [ -d $dir/kafka ];then
        printf "**********停止启动间隔过短kafka启动失败时再执行一次start即可**********\n"
        export JAVA_HOME=$dir'/jdk'
        PIDS=$(ps ax | grep -i 'quorum.QuorumPeerMain' | grep java | grep -v grep | awk '{print $1}')
        if [ -z "$PIDS" ];then
            printf "==========启动zookeeper==========\n"
            `$dir/kafka/bin/zookeeper-server-start.sh -daemon $dir/kafka/config/zookeeper.properties`
        else printf "==========zookeeper已启动==========\n"
        fi
        PIDS=$(ps ax | grep -i 'kafka.Kafka' | grep java | grep -v grep | awk '{print $1}')
        if [ -z "$PIDS" ];then
            printf "===========启动kafka==========\n"
            printf '请输入kafka的JMX端口:\n'
            read port
            `JMX_PORT=${port} $dir/kafka/bin/kafka-server-start.sh -daemon $dir/kafka/config/server.properties`
        else printf "==========kafka已启动==========\n"
        fi
    else
        printf "$dir/kafka is not exit...\n"
    fi
    elif [ "$1" == '-stop' ];then
    printf "==========停止kafka==========\n"
    PIDS=$(ps ax | grep -i 'kafka.Kafka' | grep java | grep -v grep | awk '{print $1}')
    if [ ! -z "$PIDS" ];then
        for pid in $PIDS
        do
            kill -9 $pid
        done
    fi
    printf "==========停止zookeeper==========\n"
    PIDS=$(ps ax | grep -i 'quorum.QuorumPeerMain' | grep java | grep -v grep | awk '{print $1}')
    if [ ! -z "$PIDS" ];then
        for pid in $PIDS
        do
            kill -9 $pid
        done
    fi
else
    printf "commmand $1 is not defined...\n"
    usage
    exit 1
fi
