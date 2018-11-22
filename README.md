# 准备mysql数据库

## 使用已有的MYSQL

1. 新建库并初始化数据  

```
mysql -u用户 -h地址 -p
#输入密码
mysql> create database sdas;
mysql> use sdas;
mysql> source xxx/cii_da/config/sdas.sql
mysql> exit;
```  

2. 修改采集平台配置文件 xxx/cii_da/conf/server.conf：  

```
dbport #mysql数据库端口,默认3306
dbuser #mysql用户,默认lsjcj
dbpass #mysql用户密码
```  

## 需要重新安装MYSQL  

> 安装脚本自带初始化数据库及数据

```
xxx/cii_da/bin/mysql.sh  #安装mysql
xxx/cii_da/bin/un_mysql.sh  #卸载mysql
```

# 系统安装卸载更新

> * 系统安装中会更新/etc/hosts文件(添加机器名和ip地址的映射)，方便kafka客户端访问;
> * 安装前请确认xxx/ciii_da/bin/params文件是否有参数需要修改；

## 单机

```
xxx/cii_da/bin/single.sh
-i 安装并启动
-un 卸载
-up patch.tar.gz 更新补丁
```

## 集群

```
xxx/cii_da/bin/cluster.sh
-i 安装并启动
-un 卸载
-up patch.tar.gz 更新补丁
-s 启动slave的reset服务
```  

> 注意：  
> * 操作前需要配置xxx/cii_da/bin/nodes文件,文件内有说明如何配置；  
> * 建议配置master到其他机器的ssh免密登陆(包括master到master)，否则安装过程中需要多次输入密码；  
>```
>ssh-keygen -t rsa  #一路回车即可
>ssh-copy-id -i ~/.ssh/id_rsa.pub root@xx.xx.xx.xx #将生成的公钥复制到xx.xx.xx.xx上(本机也需复制一份)
>```
> 上述命令为ssh免密登陆样例

# 补丁压缩包格式
* patch_yyyyMMdd.tar.gz  

> yyyyMMdd对应于待更新的版本yyyyMMdd,不是补丁包的打包日期

* 补丁包目录结构如下(不要求全部都有):  

```
lib/       目录，存放第三方jar
cii_da.jar 采集系统后台jar文件
web/       目录，存放编译后的页面静态文件
```  

* 更新完成后会在xxx/cii_da目录下生成update.log文件，内容如：  
`yyyy-mm-dd,HH:mm:ss=>补丁包名(patch_yyyyMMdd)`

# 辅助脚本说明  

```
xxx/cii_da/bin/cii_start.sh|cii_stop.sh #采集服务单独启动停止
xxx/cii_da/bin/kazk.sh  #kafka/zookeepr启动停止
```  
> 单机模式启动 `xx/cii_start.sh single`  
集群模式启动 `xx/cii_start.sh master`  
启动kafka `xx/kazk.sh -start ka`  
启动zookeeper `xx/kazk.sh -start zk`

# web访问方式

浏览器访问http://ip:port/static 访问数据采集系统平台(集群访问master机器)  

# 目录文件结构说明  

* 目录结构说明：  

```
bin 脚本目录；
conf 配置目录；
config mysql,kafka配置文件；
lib 第三方jar;
libs jdk,kafka,logstash压缩包；
mysql5.7.20 mysql安装包；
jdk jdk解压目录；
kafka kafka解压目录；
logstash logstash解压目录；
web 静态页面目录；
app 流计算服务目录；
da 采集服务目录；
logs 运行日志目录；  
```
* 文件说明:  

```
bin/nodes 集群安装时各节点配置
bin/params 系统安装参数配置
conf/server.conf 采集平台配置文件
config/sdas.sql 数据库初始化文件
config/my.cnf mysql数据库配置文件
config/server.properties kafka初始配置文件
config/zookeeper.properties zk初始配置文件
```

