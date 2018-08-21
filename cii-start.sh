#!/usr/bin/env bash

declare -a address

usage(){
    printf "USAGE: $0 master|slave|single\n"
}

readNodes(){
    if [ -f $1/bin/nodes ];then
        local i=0
        while read line
        do
            if [ ${#line} -eq 0 ];then
                continue
                elif [ ${line:0:1} == '#' ];then
                continue
            fi
            arr=(${line//;/ })
            if [ "${arr[1]}" != 'master' ];then
                address[$i]="${arr[0]}"
            fi
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
        for v in ${address[*]};do
            if [[ ${#params} -eq 0 ]];then
                params="$v"
            else
                params=${params}' '$v
            fi
        done
        nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer 'master' $params > "$log_dir/cii.out" 2>&1 < /dev/null &
        # nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer 'master' > "$log_dir/cii.out" 2>&1 < /dev/null &
        elif [ "$1" == 'slave' ];then
        nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer 'slave' > "$log_dir/cii.out" 2>&1 < /dev/null &
        elif [ "$1" == 'single' ];then
        nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer 'single' > "$log_dir/cii.out" 2>&1 < /dev/null &
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
