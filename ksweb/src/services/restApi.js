import { notification } from 'antd';
import { request } from './request';
// json的key值对应配置文件的key,将_换成.即可,没有下划线的key值可忽略
// orginfilter默认允许的method为get,post,head
// const ksUrl = 'http://localhost:12583'

export async function getVersion() {
  return request('/getVersion', {
    method: 'get',
    headers: {
      'Content-Type': 'application/json'
    },
  }, '获取版本号出错!');
}

export async function getGeneralInfo() {
  return request('/getGeneral', {
    method: 'get',
    headers: {
      'Content-Type': 'application/json'
    },
  }, "请求出错!");
}

export async function getTasksInfo() {
  return request('/getTasks', {
    method: 'get',
    headers: {
      'Content-Type': 'application/json'
    },
  }, "请求出错!");
}
export async function deleteTask(id, name) {
  // console.log(id, name);
  return request('/deleteTask', {
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ 'application_id': id, 'application_name': name }),
  }, "删除" + name + "任务出错!");
}
export async function storeTask(obj) {
  // console.log(JSON.stringify(obj));
  if (JSON.stringify(obj.main) === '{}') {
    notification['error']({
      message: '错误',
      description: 'KS参数不可为空',
      duration: 1.5
    });
    return;
  }
  if (JSON.stringify(obj.sources) === '[]') {
    notification['error']({
      message: '错误',
      description: '数据源不可为空',
      duration: 1.5
    });
    return;
  }
  if (JSON.stringify(obj.output) === '{}') {
    notification['error']({
      message: '错误',
      description: '输出不可为空',
      duration: 1.5
    });
    return;
  }
  return request('/storeTask', {
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(obj),
  }, "保存" + obj.main.application_name + "任务出错!");
}
export async function startTask(id, name) {
  return request('/startTask', {
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ 'application_id': id, 'application_name': name }),
  }, "启动" + name + "任务出错!");
}
export async function stopTask(id, name) {
  return request('/stopTask', {
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ 'application_id': id, 'application_name': name }),
  }, "停止" + name + "任务出错!");
}