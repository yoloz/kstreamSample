#!/bin/bash

dir=$(cd `dirname $0`/..;pwd)
 
#mysql安装
function mysql_install() {

mariabd_lib=$(rpm -qa|grep mariadb-libs)
for lib in ${mariabd_lib}
do
  rpm -e ${lib} --nodeps
done

mysql_lib=$(rpm -qa|grep mysql)
for lib in ${mysql_lib}
do
  rpm -e ${lib} --nodeps
done

libaio=$(rpm -qa|grep libaio)
if [ ! $? -eq 0 ];then
	rpm -ivh $dir/mysql5.7.20/libs/libaio-*.rpm
fi
nettools=$(rpm -qa|grep net-tools)
if [ ! $? -eq 0 ];then
	rpm -ivh $dir/mysql5.7.20/libs/net-tools-*.rpm
fi
numactl=$(rpm -qa|grep numactl)
if [ ! $? -eq 0 ];then
	rpm -ivh $dir/mysql5.7.20/libs/numactl-*.rpm --force --nodeps
fi

rpm -ivh $dir/mysql5.7.20/mysql-community-common-5.7.20-1.el6.x86_64.rpm
rpm -ivh $dir/mysql5.7.20/mysql-community-libs-5.7.20-1.el6.x86_64.rpm
rpm -ivh $dir/mysql5.7.20/mysql-community-client-5.7.20-1.el6.x86_64.rpm
rpm -ivh $dir/mysql5.7.20/mysql-community-devel-5.7.20-1.el6.x86_64.rpm
rpm -ivh $dir/mysql5.7.20/mysql-community-server-5.7.20-1.el6.x86_64.rpm --force --nodeps

now=$(date +%Y%m%d%H%M%S)
mv /etc/my.cnf /etc/my.cnf.$now
cp $dir/config/my.cnf /etc/
}


#mysql设置
function mysql_config() {
mysqld --initialize
mysqld_safe --skip-grant-tables &
printf "\n"
<<EOF
use mysql;
flush privileges;
update user set authentication_string=password('unimas') where user='root';
flush privileges;
quit
EOF

service mysqld start
mysql -uroot -punimas <<EOF
flush privileges;
alter user 'root'@'localhost' identified by 'unimas';
grant all privileges on *.* to 'root'@'%' identified by 'unimas';
flush privileges;
quit
EOF
}


#mysql创建账号和数据导入
function mysql_user() {
mysql -uroot -punimas <<EOF
flush privileges;
create user 'lsjcj'@'%' identified by 'ciilsjcj'; 
grant all privileges on *.* to 'lsjcj'@'%' identified by 'ciilsjcj';
quit
EOF
#mysql数据
mysql -uroot -punimas <<EOF
create database sdas;
use sdas;
source $dir/config/sdas.sql;
quit
EOF
service mysqld stop
kill -9 `ps -ef | grep mysqld | grep -v grep | awk '{print $2}' | xargs`
rm -rf /var/run/mysqld
mkdir /var/run/mysqld
chown mysql:mysql /var/run/mysqld/
rm -rf /var/lib/mysql/mysql.sock
rm -rf /var/lib/mysql/ib_logfile*
service mysqld start
}


#执行安装
function install() {
mysql_install
# >>${path}/logs/mysql_${date}.log 2>&1
mysql_config
# >>${path}/logs/mysql_${date}.log 2>&1
mysql_user
}


#安装
install

if [ ! $? -eq 0 ];then
	echo "Install filled!"
else
	echo "Install success!"
fi

