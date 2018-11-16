## KStream

### 结构如下：
```
KStream/
  README.md
  pom.xml
  app/
  logs/
  bin/
  config/
  libs/
    zbus/
  src/
    main/
    test/
```

- `bin` scripts of KS.
- `config` configuration of KS.
- `libs` dependencies of KS.
- `src` java source of KS.
- `app` application conf path.
- `logs` server logs

### 使用说明：
启动ks-server,具体使用参见bin里的说明,默认端口12583,修改在文件config/server.properties.
启动后页面访问`http://ip:12583/api`.说明如下：

| *api* | *描述* |
|:-----:|:-------------:|
|/cii/ks/orderApp|保存操作顺序|
|/cii/ks/storeApp|保存信息|
|/cii/ks/deleteApp|删除任务|
|/cii/ks/deployApp|部署任务|
|/cii/ks/startApp|启动任务|
|/cii/ks/stopApp|停止任务|
|/cii/ks/getApp|查询配置信息|
|/cii/ks/getAppSys|查询任务信息|
|/cii/ks/getAllAppSys|获取所有任务信息|
|/cii/ka/getAllTopics|获取所有主题|
|/cii/ka/getTopic|查询主题详情|
|/cii/ka/logEndOffset|查询主题日志偏移量|


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
