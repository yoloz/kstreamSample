#!/usr/bin/env bash
declare -a ipArr

getLocalAddr(){
    local i=0
    ips=`ip addr show|grep -v 'inet6'|grep 'inet'|awk '{print $2}'|awk -F/ '{print $1}'`
    for ip in $ips;do
        ipArr[$i]="$ip"
        let i++;
    done
}

usage(){
    printf  "USAGE: $0 option\n"
    printf  "       -i 安装并启动\n"
    printf  "       -un 卸载\n"
    printf  "       -up patch.tar.gz 更新补丁\n"
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

dir=$(cd `dirname $0`/..;pwd)

if [ "$1" == '-i' ];then
    echo "##################install jdk###################"
    if [ -d $dir/jdk ];then
        rm -rf $dir/jdk
    fi
    tar -zxpf $dir/libs/jdk-8u151-linux-x64.tar.gz -C $dir/
    mv $dir/jdk1.8.0_151 $dir/jdk
    echo "##################install kafka###################"
    if [ -d $dir/kafka ];then
        rm -rf $dir/kafka
    fi
    tar -zxpf $dir/libs/kafka_2.11-0.11.0.1.tgz -C $dir/
    mv $dir/kafka_2.11-0.11.0.1 $dir/kafka
    printf '请输入kafka数据目录:\n'
    read kaPath
    mkdir -p ${kaPath}
    printf '请输入kafka监听端口:\n'
    read kaPort
    printf '请输入kafka的JMX端口:\n'
    read jmxPort
    printf '请输入zookeeper数据目录:\n'
    read zkPath
    mkdir -p ${zkPath}
    printf '请输入zookeeper监听端口:\n'
    read zkPort
    cp $dir/config/server.properties $dir/kafka/config/server.properties
    echo "broker.id=0" >> $dir/kafka/config/server.properties
    echo "listeners=PLAINTEXT://:${kaPort}" >> $dir/kafka/config/server.properties
    echo "log.dirs=${kaPath}" >> $dir/kafka/config/server.properties
    echo "zookeeper.connect=localhost:${zkPort}/kafka" >> $dir/kafka/config/server.properties
    cp $dir/config/zookeeper.properties $dir/kafka/config/zookeeper.properties
    echo 'tickTime=2000' >> $dir/kafka/config/zookeeper.properties
    echo "dataDir=${zkPath}/data" >> $dir/kafka/config/zookeeper.properties
    echo "dataLogDir=${zkPath}/log" >> $dir/kafka/config/zookeeper.properties
    echo "clientPort=${zkPort}" >> $dir/kafka/config/zookeeper.properties
    echo "##################install logstash###################"
    if [ -d $dir/logstash ];then
        rm -rf $dir/logstash
    fi
    tar -zxpf $dir/libs/logstash-6.2.4.tar.gz -C $dir/
    mv $dir/logstash-6.2.4 $dir/logstash
    
    export JAVA_HOME=$dir'/jdk'
    echo "##################start kafka zk###################"
    `$dir/kafka/bin/zookeeper-server-start.sh -daemon $dir/kafka/config/zookeeper.properties`
    sleep 2
    `JMX_PORT=${jmxPort} $dir/kafka/bin/kafka-server-start.sh -daemon $dir/kafka/config/server.properties`
    echo "##################采集平台配置###################"
    getLocalAddr
    echo "请从下列选项中(1-${#ipArr[@]})选择采集平台的访问地址:"
    for i in $(seq 1 ${#ipArr[*]});do
        printf "$i ${ipArr[i-1]}\n"
    done
    read num
    if [ $num -lt 1 -o $num -gt ${#ipArr[*]} ];then
        echo "$num的范围在(1-${#ipArr[@]}),选择错误!"
        exit 1
    fi
    Addr=${ipArr[$num-1]}
    # printf "更新/etc/hosts文件\n"
    echo "$Addr $HOSTNAME" >> /etc/hosts
    # if [[ "$Addr" =~ ^([0-9]{1,3}.){3}[0-9]{1,3}$ ]];then
    echo "window.g = {" > $dir/web/config.js
    echo "IP:'"$Addr"'," >> $dir/web/config.js
    echo "请输入采集平台的访问端口:"
    read PORT
    echo "PORT:$PORT" >> $dir/web/config.js
    echo "}" >> $dir/web/config.js
    echo 'port='$PORT >> $dir/conf/server.conf
    printf '请输入安装mysql的机器IP:\n'
    read host
    echo 'dbhost='$host >> $dir/conf/server.conf
    # else
    #     echo "********IP格式错误(IPV4)********"
    #     exit 1
    # fi
    echo "##################启动采集平台###################"
    $dir/bin/cii-start.sh 'single'
    elif [ "$1" == '-un' ];then
    printf '请输入kafka数据目录:\n'
    read kaPath
    printf '请输入zookeeper数据目录:\n'
    read zkPath
    $dir/bin/un_install.sh ${kaPath} ${zkPath}
    elif [ "$1" == '-up' ];then
    if [ $# -lt 2 ];then
        usage
        exit 1
        elif [ ! -f $2 ];then
        printf 'patch file '$2' does not exit!\n'
        exit 1
    fi
    $dir/bin/update.sh $2 "single"
else
    printf "option $1 undefined!\n"
    usage
    exit 1
fi
