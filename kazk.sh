#!/usr/bin/env bash
usage(){
    printf  "USAGE: $0 option\n"
    printf  "       -start ka|zk 启动kafka或者zookeeper\n"
    printf  "       -stop  ka|zk  停止kafka或者zookeeper\n"
}
if [ $# -lt 2 ];then
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
zkPID(){
    # echo $(ps ax | grep -i 'quorum.QuorumPeerMain' | grep java | grep -v grep | awk '{print $1}')
    echo $(ps ax | grep -i 'zookeeper-gc.log' | grep java | grep -v grep | awk '{print $1}')
}
kaPID(){
    # echo $(ps ax | grep -i 'kafka.Kafka' | grep java | grep -v grep | awk '{print $1}')
    echo $(ps ax | grep -i 'kafkaServer-gc.log' | grep java | grep -v grep | awk '{print $1}')
}
dir=`cd $(dirname $0)/..;pwd`
if [ "$1" == '-start' ];then
    if [ -d $dir/kafka -a -d $dir/jdk ];then
        export JAVA_HOME=$dir'/jdk'
        if [ "$2" == 'ka' ];then
            PIDS=`zkPID`
            if [ -z "$PIDS" ];then
                printf "请先启动zookeeper\n"
                exit 1
            fi
            PIDS=`kaPID`
            if [ -z "$PIDS" ];then
                printf '请输入kafka的JMX端口:\n'
                read port
                `JMX_PORT=${port} $dir/kafka/bin/kafka-server-start.sh -daemon $dir/kafka/config/server.properties`
                printf "kafka启动完成\n"
            else printf "kafka已启动\n"
            fi
            elif [ "$2" == 'zk' ];then
            PIDS=`zkPID`
            if [ -z "$PIDS" ];then
                `$dir/kafka/bin/zookeeper-server-start.sh -daemon $dir/kafka/config/zookeeper.properties`
                printf "zookeeper启动完成\n"
            else printf "zookeeper已启动\n"
            fi
        else
            printf "param $2 is undefined!"
            usage
            exit 1
        fi
    else
        printf "$dir/kafka or $dir/jdk is not exit...\n"
    fi
    elif [ "$1" == '-stop' ];then
    if [ "$2" == 'ka' ];then
        PIDS=`kaPID`
        if [ ! -z "$PIDS" ];then
            for pid in $PIDS
            do
                kill -9 $pid
            done
        fi
        printf "kafka进程退出\n"
        elif [ "$2" == 'zk' ];then
        PIDS=`kaPID`
        if [ ! -z "$PIDS" ];then
            printf "请先停止kafka\n"
            exit 1
        fi
        PIDS=`zkPID`
        if [ ! -z "$PIDS" ];then
            for pid in $PIDS
            do
                kill -9 $pid
            done
        fi
        printf "zookeeper进程退出\n"
    fi
else
    printf "commmand $1 is not defined...\n"
    usage
    exit 1
fi
