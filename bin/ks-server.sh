#!/usr/bin/env bash
if [ $# -lt 1 ]; then
   echo "USAGE: $0 start|stop|daemon"
   exit 1
fi
case $1 in
  -h | --h | --help)
   echo "USAGE: $0 start|stop|daemon"
   exit 1
  ;;
  *)
  ;;
esac
. `dirname $0`/env.sh
log_dir=${KS_DIR}/logs
#echo "Log recorded in $log_dir"
test -d ${log_dir} || mkdir -p ${log_dir}
if [ -f ${KS_DIR}/config/log4j.properties ]; then
   KS_LOG4J_OPTS="-Dlog4j.configuration=file:$KS_DIR/config/log4j.properties"
else
   echo "can't find file $KS_DIR/config/log4j.properties..."
   exit 1
fi
KS_LOG4J_OPTS="-Dks.logs.dir=$log_dir $KS_LOG4J_OPTS"
KS_ROOT_DIR="-Dks.root.dir=$KS_DIR"
if [ "x$1" = "xdaemon" ]; then
  shift
  nohup ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.kska.KsServer "start" > "$log_dir/ks.out" 2>&1 < /dev/null &
else
  exec ${JAVA} ${KS_ROOT_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.kska.KsServer "$@"
fi

