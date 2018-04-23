#!/usr/bin/env bash
if [ $# -lt 1 ]; then
   echo "USAGE: $0 main.properties start|test|stop"
   exit 1
fi
case $1 in
  -h | --h | --help)
   echo "USAGE: $0 main.properties start|test|stop"
   exit 1
  ;;
  *)
  ;;
esac
if [ ! -f $1 ]; then
   echo "$1 does not exit or empty..."
   echo "USAGE: $0 main.properties start|test|stop"
   exit 1
fi
. `dirname $0`/env.sh
app_dir=$(cd `dirname $1`;pwd)
log_dir=${app_dir}/logs
#echo "Log recorded in $log_dir"
test -d ${log_dir} || mkdir -p ${log_dir}
if [ -f ${KS_DIR}/config/log4j.properties ]; then
   KS_LOG4J_OPTS="-Dlog4j.configuration=file:$KS_DIR/config/log4j.properties"
else
   echo "can't find file log4j.properties..."
   exit 1
fi
KS_LOG4J_OPTS="-Dks.logs.dir=$log_dir $KS_LOG4J_OPTS"
KS_APP_DIR="-Dks.app.dir=$app_dir"
#exec ${JAVA} ${KS_APP_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH} com.unimas.kstream.AppServer "$@"
nohup ${JAVA} ${KS_APP_DIR} ${KS_LOG4J_OPTS} -cp ${CLASSPATH}  com.unimas.kstream.AppServer "$@" > "$log_dir/ks.out" 2>&1 < /dev/null &
