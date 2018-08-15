#!/usr/bin/env bash
#####################运行前请配置好ssh免密登陆

dir=`cd $(dirname $0)/..;pwd`
sshUser='root'
declare -a nodeConf
declare -a zkConf
declare -a kaConf

usage(){
    printf  "USAGE: $0 option\n"
    printf  "       -i 安装并启动\n"
    printf  "       -un 卸载\n"
    printf  "       -up patch.tar.gz 更新补丁\n"
}
readNodes(){
    local i=0
    while read line
    do
        if [ ${#line} -eq 0 ];then
            continue
            elif [ ${line:0:1} == '#' ];then
            continue
        fi
        nodeConf[$i]="$line"
        let i++
    done < $dir/bin/nodes
    readonly nodeConf
}
confZK(){
    if [ $[${#nodeConf[*]}%2] -eq 0 -a $[${#nodeConf[*]}-1] -le 1 ];then
        printf '请输入zookeeper数据目录:\n'
        read path
        zkConf[0]="$path"
        printf '请输入zookeeper监听端口:\n'
        read port
        zkConf[1]="$port"
    else
        printf '请输入zookeeper数据目录:\n'
        read path
        zkConf[0]="$path"
        printf '请输入zookeeper监听端口:\n'
        read port
        zkConf[1]="$port"
        printf '请输入zookeeper同步端口:\n'
        read port
        zkConf[2]="$port"
        printf '请输入zookeeper选举端口:\n'
        read port
        zkConf[3]="$port"
    fi
    readonly zkConf
}
confKA(){
    printf '请输入kafka数据目录:\n'
    read path
    kaConf[0]="$path"
    printf '请输入kafka监听端口:\n'
    read port
    kaConf[1]="$port"
    printf '请输入kafka的JMX端口:\n'
    read port
    kaConf[2]="$port"
    readonly kaConf
}
getZKConnect(){
    local zc=''
    for l in ${nodeConf[@]};do
        params=(${l//;/ })
        if [ ${#zkConf[@]} -eq 2 -a "${params[1]}" != 'master' ];then
            zc=${params[0]}':'${zkConf[1]}
            elif [[ ${#nodeConf[*]} -gt 2 && $[${#nodeConf[*]}%2] -eq 0 && "${params[1]}" != 'master' ]];then
            if [[ ${#zc} -eq 0 ]];then
                zc=${params[0]}':'${zkConf[1]}
            else
                zc=${zc}','${params[0]}':'${zkConf[1]}
            fi
            elif [[ ${#nodeConf[*]} -gt 2 && $[${#nodeConf[*]}%2] != 0 ]];then
            if [[ ${#zc} -eq 0 ]];then
                zc=${params[0]}':'${zkConf[1]}
            else
                zc=${zc}','${params[0]}':'${zkConf[1]}
            fi
        fi
    done
    echo ${zc}'/kafka'
}
createKAConf(){
    printf '********生成kafka集群配置********\n'
    local _zk=`getZKConnect`
    local i=0
    while [[ i -lt ${#nodeConf[*]} ]];do
        params=(${nodeConf[i]//;/ })
        fn=$dir/clusterConf/${params[0]}'server.properties'
        cp $dir/config/server.properties $dir/clusterConf
        mv $dir/clusterConf/server.properties $fn
        echo "broker.id=$i" >> $fn
        echo "listeners=PLAINTEXT://${params[0]}:${kaConf[1]}" >> $fn
        echo "log.dirs=${kaConf[0]}" >> $fn
        echo "zookeeper.connect=$_zk" >> $fn
        let i++
    done
}
getZKSP(){
    local zsp=''
    local i=0
    while [[ i -lt ${#nodeConf[*]} ]];do
        params=(${nodeConf[i]//;/ })
        if [[ $[${#nodeConf[*]}%2] -eq 0 && "${params[1]}" != 'master' ]];then
            if [ ${#zsp} -eq 0 ];then
                zsp='server.'$i'='${params[0]}':'${zkConf[2]}':'${zkConf[3]}
            else
                zsp=${zsp}';server.'$i'='${params[0]}':'${zkConf[2]}':'${zkConf[3]}
            fi
            elif [[ $[${#nodeConf[*]}%2] != 0 ]];then
            if [ ${#zsp} -eq 0 ];then
                zsp='server.'$i'='${params[0]}':'${zkConf[2]}':'${zkConf[3]}
            else
                zsp=${zsp}';server.'$i'='${params[0]}':'${zkConf[2]}':'${zkConf[3]}
            fi
        fi
        let i++
    done
    echo $zsp
}
createZKConf(){
    printf '********生成ZK配置文件********\n'
    if [[ ${#zkConf[@]} -eq 2 ]];then
        cp $dir/config/zookeeper.properties $dir/clusterConf
        fn=$dir/clusterConf/zookeeper.properties
        echo 'tickTime=2000' >> $fn
        echo "dataDir=${zkConf[0]}/data" >> $fn
        echo "dataLogDir=${zkConf[0]}/log" >> $fn
        echo "clientPort=${zkConf[1]}" >> $fn
    else
        local i=0
        local zkSP=`getZKSP`
        local _zsp=(${zkSP//;/ })
        while [[ i -lt ${#nodeConf[*]} ]];do
            params=(${nodeConf[i]//;/ })
            fn=$dir/clusterConf/${params[0]}'zookeeper.properties'
            cp $dir/config/zookeeper.properties $dir/clusterConf
            mv $dir/clusterConf/zookeeper.properties $fn
            echo "clientPort=${zkConf[1]}" >> $fn
            echo "dataDir=${zkConf[0]}/data" >> $fn
            echo "dataLogDir=${zkConf[0]}/log" >> $fn
            echo 'tickTime=2000' >> $fn
            echo 'initLimit=5' >> $fn
            echo 'syncLimit=2' >> $fn
            for v in ${_zsp[@]};do
                echo $v >> $fn
            done
            let i++
        done
    fi
}
confServer(){
    printf '********采集平台配置********\n'
    printf '请输入安装mysql的机器IP:\n'
    read host
    echo 'dbhost='$host >> $dir/conf/server.conf
    printf '请输入采集平台web端口:\n'
    read port
    echo 'port='$port >> $dir/conf/server.conf
    echo "window.g = {" > $dir/web/config.js
    echo "PORT:$port," >> $dir/web/config.js
}
copyResource(){
    scp $dir/bin/cii-start.sh $sshUser@$2:$1/bin
    scp $dir/bin/cii-stop.sh $sshUser@$2:$1/bin
    scp $dir/bin/env.sh $sshUser@$2:$1/bin
    scp $dir/bin/kazk.sh $sshUser@$2:$1/bin
    scp $dir/bin/ks-app.sh $sshUser@$2:$1/bin
    scp $dir/bin/un_install.sh $sshUser@$2:$1/bin
    scp $dir/bin/update.sh $sshUser@$2:$1/bin
    if [ "$3" == 'master' ];then
        scp $dir/bin/cluster.sh $sshUser@$2:$1/bin
        scp $dir/bin/mysql.sh $sshUser@$2:$1/bin
        scp $dir/bin/nodes $sshUser@$2:$1/bin
        scp $dir/bin/single.sh $sshUser@$2:$1/bin
        scp $dir/bin/un_mysql.sh $sshUser@$2:$1/bin
    fi
    scp -r $dir/conf $sshUser@$2:$1
    scp -r $dir/lib $sshUser@$2:$1
    scp $dir/libs/jdk-8u151-linux-x64.tar.gz $sshUser@$2:$1
    scp $dir/libs/kafka_2.11-0.11.0.1.tgz $sshUser@$2:$1
    scp $dir/libs/logstash-6.2.4.tar.gz $sshUser@$2:$1
    ssh $sshUser@$2 "tar -zxpf $1/jdk-8u151-linux-x64.tar.gz -C $1 && mv $1/jdk1.8.0_151 $1/jdk"
    ssh $sshUser@$2 "tar -zxpf $1/kafka_2.11-0.11.0.1.tgz -C $1 && mv $1/kafka_2.11-0.11.0.1 $1/kafka"
    ssh $sshUser@$2 "tar -zxpf $1/logstash-6.2.4.tar.gz -C $1 && mv $1/logstash-6.2.4 $1/logstash"
    scp $dir/clusterConf/$2'server.properties' $sshUser@$2:$1/kafka/config
    ssh $sshUser@$2 "mv $1/kafka/config/$2server.properties $1/kafka/config/server.properties"
    if [[ -f $dir/clusterConf/$2'zookeeper.properties' ]];then
        scp $dir/clusterConf/$2'zookeeper.properties' $sshUser@$2:$1/kafka/config
        ssh $sshUser@$2 "mv $1/kafka/config/$2zookeeper.properties $1/kafka/config/zookeeper.properties"
    fi
}
remoteZK(){
    if [ ${#zkConf[@]} -eq 2 -a "$3" != 'master' ];then
        scp $dir/clusterConf/zookeeper.properties $sshUser@$1:$2/kafka/config
        ssh $sshUser@$1 "rm -rf ${zkConf[0]} && mkdir -p ${zkConf[0]}/data && mkdir -p ${zkConf[0]}/log"
        ssh $sshUser@$1 "export JAVA_HOME=$2/jdk && $2/kafka/bin/zookeeper-server-start.sh -daemon $2/kafka/config/zookeeper.properties"
        printf "********启动$1的zk********\n"
        elif [[ ${#nodeConf[*]} -gt 2 && $[${#nodeConf[*]}%2] -eq 0 && "$3" != 'master' ]];then
        ssh $sshUser@$1 "rm -rf ${zkConf[0]} && mkdir -p ${zkConf[0]}/data && mkdir -p ${zkConf[0]}/log && echo $4 > ${zkConf[0]}/data/myid"
        ssh $sshUser@$1 "export JAVA_HOME=$2/jdk && $2/kafka/bin/zookeeper-server-start.sh -daemon $2/kafka/config/zookeeper.properties"
        printf "********启动$1的zk********\n"
        elif [[ ${#nodeConf[*]} -gt 2 && $[${#nodeConf[*]}%2] != 0 ]];then
        ssh $sshUser@$1 "rm -rf ${zkConf[0]} && mkdir -p ${zkConf[0]}/data && mkdir -p ${zkConf[0]}/log && echo $4 > ${zkConf[0]}/data/myid"
        ssh $sshUser@$1 "export JAVA_HOME=$2/jdk && $2/kafka/bin/zookeeper-server-start.sh -daemon $2/kafka/config/zookeeper.properties"
        printf "********启动$1的zk********\n"
    fi
}
main(){
    if [ $# -lt 1 ];then
        usage
        exit 1
        elif [ "$1" == '-up' -a $# -lt 2 ];then
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
    readNodes
    if [ ${#nodeConf[*]} -le 1 ];then
        printf '至少两台机器\n'
        exit 1
    fi
    printf '请输入各机器ssh免密登陆用户名:\n'
    read userName
    sshUser=$userName
    readonly sshUser
    if [ "$1" == '-i' ];then
        confZK
        confKA
        confServer
        rm -rf $dir/clusterConf
        mkdir $dir/clusterConf
        createKAConf
        createZKConf
        local i=0
        local address=[]
        while [[ i -lt ${#nodeConf[*]} ]];do
            params=(${nodeConf[i]//;/ })
            target=${params[2]}'/cii_da'
            ssh $sshUser@${params[0]} "mkdir -p ${params[2]}/cii_da/bin"
            copyResource $target ${params[0]} ${params[1]}
            if [ "${params[1]}" == 'master' ];then
                echo "IP:'"${params[0]}"'" >> $dir/web/config.js
                echo "}" >> $dir/web/config.js
                scp -r $dir/web $sshUser@${params[0]}:$target
            fi
            ssh $sshUser@${params[0]} "rm -rf ${kaConf[0]} && mkdir -p ${kaConf[0]}"
            remoteZK ${params[0]} $target ${params[1]} $i
            if [ "${params[1]}" != 'master' ];then
                address[$i]="${params[0]}"
                printf "********启动${params[0]}的采集平台[slave]********\n"
                ssh $sshUser@${params[0]} "$target/bin/cii-start.sh daemon slave"
            fi
            let i++
        done
        for v in ${nodeConf[@]}
        do
            params=(${v//;/ })
            target=${params[2]}'/cii_da'
            printf "********启动${params[0]}的kafka********\n"
            ssh $sshUser@${params[0]} "export JAVA_HOME=$target/jdk && JMX_PORT=${kaConf[2]} $target/kafka/bin/kafka-server-start.sh -daemon $target/kafka/config/server.properties"
            if [ "${params[1]}" == 'master' ];then
                printf "********启动${params[0]}的采集平台[master]********\n"
                local params=''
                for v in ${address[*]};do
                    if [[ ${#params} -eq 0 ]];then
                        params="$v"
                    else
                        params=${params}' '$v
                    fi
                done
                ssh $sshUser@${params[0]} "$target/bin/cii-start.sh daemon master $params"
            fi
        done
        # rm -rf $dir/clusterConf
        rm -rf $dir
        elif [ "$1" == '-un' ];then
        printf '请输入kafka数据目录:\n'
        read kaPath
        printf '请输入zookeeper数据目录:\n'
        read zkPath
        for v in ${nodeConf[*]}
        do
            params=(${v//;/ })
            target=${params[2]}'/cii_da'
            printf "********卸载${params[0]}********\n"
            ssh $sshUser@${params[0]} "$target/bin/un_install.sh $kaPath $zkPath"
        done
        elif [ "$1" == '-up' ];then
        if [ ! -f $2 ];then
            printf 'patch file '$2' does not exit!\n'
            exit 1
        fi
        for v in ${nodeConf[@]}
        do
            params=(${v//;/ })
            target=${params[2]}'/cii_da'
            printf "********更新${params[0]}********\n"
            scp $2 $sshUser@${params[0]}:$target
            ssh $sshUser@${params[0]} "$target/bin/update.sh $target/`basename $2` ${params[1]}"
        done
    else
        printf "option $1 undefined!\n"
        usage
        exit 1
    fi
}
main "$@"
