import React, { Component } from 'react';
import { Table, Popconfirm, Icon, Button, notification, } from 'antd';
import { deleteTask, getTasksInfo } from '../services/restApi';


class CfgMgrTable extends Component {

  constructor(props) {
    super(props);
    this.state = {
      data: [],
    };
    this.columns = [{
      title: '任务名称',
      dataIndex: 'main',
      width: '25%',
      render: (text, record) => {
        return text.application_name;
      }
    }, {
      title: '数据源',
      dataIndex: 'sources',
      width: '20%',
      render: (text, record) => {
        return (
          <Button type="primary" ghost icon="file-text" onClick={() => this.toNewTask(1, record)}></Button>
        );
      }
    }, {
      title: '操作步骤',
      dataIndex: 'operations',
      width: '20%',
      render: (text, record) => {
        return (
          <Button type="primary" ghost icon="file-text" onClick={() => this.toNewTask(2, record)}></Button>
        );
      }
    }, {
      title: '输出',
      dataIndex: 'outputs',
      width: '20%',
      render: (text, record) => {
        return (
          <Button type="primary" ghost icon="file-text" onClick={() => this.toNewTask(3, record)}></Button>
        );
      }
    }, {
      title: '删除',
      dataIndex: 'delete',
      width: '15%',
      render: (text, record) => {
        return (
          // this.state.data.length > 1 ? (
          <Popconfirm title="确认删除吗?" onConfirm={() => this.deleteOneTask(record.main)}>
            <Icon type="delete" style={{ cursor: 'pointer', fontSize: 16, color: '#08c' }} /></Popconfirm>
          // ) : null
        );
      }
    }];
  }
  toNewTask = (step, record) => {
    const { toNewTaskPage } = this.props
    toNewTaskPage(step, record);
  };
  deleteOneTask = (record) => {
    let that = this;
    deleteTask(record.application_id, record.application_name).then((data) => {
      if (data.success) {
        const dataSource = [...that.state.data];
        that.setState({ data: dataSource.filter(item => item.main.application_id !== record.application_id) });
        notification.success({ message: "删除成功", duration: 1, });
      } else {
        notification['error']({
          message: '异常',
          description: data.error,
          duration: 1.5
        });
      }
    });
    // const dataSource = [...that.state.data];
    // that.setState({ data: dataSource.filter(item => item.application_id !== record.application_id) });
    // notification.success({ message: "删除成功", duration: 1, })
  }
  fetchData() {
    getTasksInfo().then((data) => {
      if (data.success) {
        let results = data.results;//JSON.parse(data.results);
        // console.log(results);
        this.setState({
          data: results,
        });
        // } else { //测试
        //   let date = new Date();
        //   let dataArr = [];
        //   for (let i = 1; i < 5; i++) {
        //     dataArr.push({
        //       main: {
        //         application_id: i,
        //         application_name: date.toString() + ` ${i}`,
        //         bootstrap_servers: '127.0.0.1:9092',
        //       },
        //       application_name: date.toString() + ` ${i}`,
        //       application_id: i,
        //       sources: [{
        //         ks_name: 'test1',
        //         ks_type: 'stream',
        //         ks_topics: 'test1',
        //       }, {
        //         ks_name: 'test2',
        //         ks_type: 'table',
        //         ks_topics: 'test2',
        //         ks_time_name: 'time'
        //       }],
        //       operations: [{
        //         operation_ks_name: 'test1',
        //         join_ks_name: '',

        //       }],
        //       output: {},
        //     });
        //   }
        //   this.setState({
        //     data: dataArr,
        //   });
      } else {
        notification['error']({
          message: '异常',
          description: data.error,
          duration: 1.5
        });
      }
    });
  }
  componentDidMount() { this.fetchData() }
  render() {
    return (
      <Table
        columns={this.columns}
        rowKey={record => record.main.application_id}
        dataSource={this.state.data}
        // loading={this.state.loading}
        pagination={{ pageSize: 50 }}
        scroll={{ y: 400 }}
        size="middle"
      />
    );
  }
}

export { CfgMgrTable };