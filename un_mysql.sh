#!/bin/bash

service mysqld stop

rpm -ev mysql-community-client-5.7.20-1.el6.x86_64 --nodeps
rpm -ev mysql-community-common-5.7.20-1.el6.x86_64 --nodeps
rpm -ev mysql-community-devel-5.7.20-1.el6.x86_64 --nodeps
rpm -ev mysql-community-libs-5.7.20-1.el6.x86_64 --nodeps
rpm -ev mysql-community-server-5.7.20-1.el6.x86_64 --nodeps
rm -rf /etc/my.cnf*
rm -rf /var/log/mysqld.log
rm -rf /usr/share/mysql
rm -rf /usr/lib64/mysql
rm -rf /var/lib/mysql
rm -rf /usr/bin/mysql
rm -rf /etc/logrotate.d/mysql
rm -rf /var/run/mysqld/mysqld.pid
rm -rf /usr/share/man/man1/mysql*

