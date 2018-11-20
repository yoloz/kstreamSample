#!/usr/bin/env bash

declare -a address
declare -a parameters

usage(){
    printf "USAGE: $0 master|slave|single\n"
}

readParams(){
    local i=0
    while read line || [[ -n ${line} ]]
    do
        if [ ${#line} -eq 0 ];then
            continue
            elif [ ${line:0:1} == '#' ];then
            continue
        fi
        parameters[$i]="$line"
        let i++
    done < $1/bin/params
    readonly parameters
}

readNodes(){
    if [ -f $1/bin/nodes ];then
        local i=0
        while read line || [[ -n ${line} ]]
        do
            if [ ${#line} -eq 0 ];then
                continue
                elif [ ${line:0:1} == '#' ];then
                continue
            fi
            # arr=(${line//;/ })
            # if [ "${arr[1]}" != 'master' ];then
            #     address[$i]="${arr[0]}"
            # fi
            # let i++
            address[$i]="$line"
            let i++
        done < $1/bin/nodes
        readonly address
    fi
}
checkRun(){
    pid=$(ps ax | grep 'com.unimas.RestServer' | grep java | grep -v grep | awk '{print $1}')
    if [ -n "$pid" ]; then
        echo "cii_da[$pid] is running......"
        exit 1
    fi
}
main(){
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
    checkRun
    . `dirname $0`/env.sh
    log_dir=${KS_DIR}/logs
    #echo "Log recorded in $log_dir"
    test -d ${log_dir} || mkdir -p ${log_dir}
    if [ -f ${KS_DIR}/conf/log4j.properties ]; then
        KS_LOG4J_OPTS="-Dlog4j.configuration=file:$KS_DIR/conf/log4j.properties"
    else
        echo "can't find file $KS_DIR/conf/log4j.properties..."
        exit 1
    fi
    KS_ROOT_DIR="-Dcii.root.dir=$KS_DIR"
    if [ "$1" == 'master' ];then
        readNodes ${KS_DIR}
        local params=''
        local masterIp=''
        for v in ${address[*]};do
            arr=(${v//;/ })
            if [ "${arr[1]}" != 'master' ];then
                if [[ ${#params} -eq 0 ]];then
                    params="${arr[0]}"
                else
                    params=${params}' '${arr[0]}
                fi
            else
                masterIp="${arr[0]}"
            fi
        done
        nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer $* \
        "$masterIp" "$HOSTNAME" $params > "$log_dir/cii.out" 2>&1 < /dev/null &
        elif [ "$1" == 'slave' ];then
        nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer $* \
        > "$log_dir/cii.out" 2>&1 < /dev/null &
        elif [ "$1" == 'single' ];then
        readParams ${KS_DIR}
        Addr=${parameters[9]}
        hostName=$HOSTNAME
        if [ "${parameters[10]}"x != "HOSTNAME"x ];then
            hostName=${parameters[10]}
        fi
        nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer $* \
        "$Addr" "$hostName" > "$log_dir/cii.out" 2>&1 < /dev/null &
    else
        printf "command $1 undefined!\n"
        usage
        exit 1
    fi
}
main "$@"
# if [ "x$1" = "x-daemon" ]; then
#     shift
#     nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer "$@" > "$log_dir/cii.out" 2>&1 < /dev/null &
# else
#     exec ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer "$@"
# fi
