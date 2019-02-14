## KStream

### 结构如下：
```
KStream/
  README.md
  pom.xml
  app/
  bin/
  config/
    back/
  libs/
    zbus/
  ksweb/
  src/
    main/
    test/
```

- `bin` scripts of KS.
- `config` configuration of KS.
  - `back` configuration details.
- `ksweb` web source of KS helper.
- `libs` dependencies of KS.
- `src` java source of KS.
- `app` application conf path.
- `web` web context path.

### 使用说明：
- 1,KS助手,首先启动ks-server-web,具体使用参见bin里的说明,
默认端口12583,如果要修改端口则页面要重新生成.启动后页面访问`http://ip:12583`
服务的创建修改启动停止等都可以在页面中操作.
启动的服务不受web-server影响,ks-server-web可随时启动停止(暂时未加入安全相关,使用完建议关闭).
- 2,手动写配置文件,可以使用bin里的ks-app-start启动,具体使用参见
bin里的说明.适用先前最新版配置文件.
- 3,对于先前最新版配置文件,web自行鉴别展示。请自行组织文件结构如下:
```
app/    安装目录下的app目录
  app_id/
     main.properties 入口文件,必须此名称
     output.properties 输出文件,必须此名称
     xxx.properties 操作数据源文件
```
>Note:main.properties添加application.name=xxxx;
操作文件添加operation.name=文件名;
数据源文件名为ks.name值;

>Note:第三点暂未测试；

### 重置已运行的任务(希望从头开始重新处理数据)
- 停止任务,记下任务的applicationId；
- 进入kafka-dir/bin,执行
```
./kafka-streams-application-reset.sh --application-id 1234 
 --input-topics t1 

```
- 手动删除state store:查看配置文件里面的state.dir目录(默认/tmp/kafka-streams/),
删除目录下的applicationId目录(如果有的话);
- 启动任务
>Note:kafka-streams-application-reset.sh参数说明：
>>* --application-id      KStream application ID(application.id),必要参数. 
>>* --bootstrap-servers   broker地址(HOST1:PORT1,HOST2:PORT2)默认:localhost:9092.              
>>* --dry-run             显示将执行的操作而不执行重置命令.                              
>>* --input-topics        输入源topics(t1,t2),将偏移量重置为最早的可用偏移量.                      
>>* --intermediate-topics 通过方法through()使用的中间topics(t1,t2),偏移量直接跳到最后.                  
>>* --zookeeper           Zookeeper地址(HOST:POST)默认:localhost:2181.deprecated in 1.0 





