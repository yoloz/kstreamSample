#!/usr/bin/env bash
case $1 in
  -h |-help| --h | --help)
   echo "USAGE: $0 [daemon]"
   exit 1
  ;;
  *)
  ;;
esac

pid=$(ps ax | grep 'com.unimas.RestServer' | grep java | grep -v grep | awk '{print $1}')
if [ -n "$pid" ]; then
   echo "cii_da[$pid] is running......"
   exit 1
fi

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

if [ "x$1" = "xdaemon" ]; then
  shift
  nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer "$@" > "$log_dir/cii.out" 2>&1 < /dev/null &
else
  exec ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.RestServer "$@"
fi
