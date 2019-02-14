## how to use scripts

- `ks-app-start` 启动停止app. 参数：app的main配置路径;start|test|stop.
  - `start`:启动
  - `stop`:停止
  - `test`：启动,测试模式(application id随即生成,停止后删除生成的中间topic)
```sh
./ks-app-start.sh /home/main.properties start
```
- `ks-server-web` 启动停止JettyServer.参数：start|stop|daemon
  - `start`:启动
  - `stop`:停止
  - `daemon`：启动,守护模式
```sh
./ks-server-web.sh start
```
- `env.sh` 判定运行环境