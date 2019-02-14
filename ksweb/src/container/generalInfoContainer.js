import React, { Component } from 'react';
import { Table, Button, Badge, notification, } from 'antd';
import { getGeneralInfo, startTask, stopTask } from '../services/restApi';

class GeneralTable extends Component {

    constructor(props) {
        super(props);
        this.state = {
            data: [],
        };
        this.columns = [{
            title: '任务名称',
            dataIndex: 'application_name',
            width: '20%',
        }, {
            title: '状态',
            dataIndex: 'application_status',
            width: '16%',
            render: (text, record) => {
                if (text === 'start') { return <Badge status='processing' text='启动中' /> }
                else if (text === 'run') { return <Badge status='processing' text='运行' /> }
                else if (text === 'odd') { return <Badge status='error' text='启动异常' /> }
                else { return <Badge status='default' text='未运行' /> }
            }
        }, {
            title: 'CPU(%)',
            dataIndex: 'application_cpu',
            width: '16%',
        }, {
            title: '内存(%)',
            dataIndex: 'application_mem',
            width: '16%',
        }, {
            title: '运行时长',
            dataIndex: 'application_time',
            width: '16%',
        }, {
            title: '操作',
            dataIndex: 'handle',
            width: '16%',
            render: (text, record) => {
                if (record.application_status === 'start') { return <Button disabled type="primary" ghost icon="play-circle"></Button> }
                else if (record.application_status === 'run') { return <Button type="primary" ghost icon="pause-circle" title='停止' onClick={() => this.stopKS(record)}></Button> }
                else { return <Button type="primary" ghost icon="play-circle" title='启动' onClick={() => this.startKS(record)}></Button> }
            }
        }];
    }
    startKS(record) {
        startTask(record.application_id, record.application_name).then((data) => {
            if (data.success) {
                notification.success({ message: "启动成功", duration: 1, });
                const { updateRunners } = this.props;
                this.fetchData(updateRunners)
            } else {
                notification['error']({
                    message: '异常',
                    description: data.error,
                    duration: 1.5
                });
            }
        });
    }
    stopKS(record) {
        stopTask(record.application_id, record.application_name).then((data) => {
            if (data.success) {
                notification.success({ message: "停止成功", duration: 1, });
                const { updateRunners } = this.props;
                this.fetchData(updateRunners)
            } else {
                notification['error']({
                    message: '异常',
                    description: data.error,
                    duration: 1.5
                });
            }
        });
    }
    fetchData(updateRunners) {
        getGeneralInfo().then((data) => {
            if (data.success) {
                let results = data.results;// JSON.parse(data.results);
                // console.log(results);
                this.setState({
                    data: results,
                });
                let runners = [];
                for (const obj of results) {
                    if (obj.application_status === 'run' || obj.application_status === 'start') {
                        runners.push(obj.application_id);
                    }
                }
                updateRunners(runners);
                // } else { //测试数据
                //     let date = new Date();
                //     let dataArr = [], runners = [];
                //     for (let i = 1; i < 5; i++) {
                //         let obj = {
                //             application_id: i,
                //             application_name: date.toString() + ` ${i}`,
                //             application_status: i % 2 === 0 ? 'run' : 'stop',
                //             application_cpu: `cpu${i}`,
                //             application_mem: `cpu${i}`,
                //         }
                //         dataArr.push(obj);
                //         if (obj.application_status === 'run') { runners.push(i); }
                //     }
                //     this.setState({
                //         data: dataArr,
                //     });
                //     updateRunners(runners);
            } else {
                notification['error']({
                    message: '异常',
                    description: data.error,
                    duration: 1.5
                });
            }
        });
    }
    componentDidMount() {
        if (window.generalInfoInterval !== undefined && window.generalInfoInterval !== 'undefined') {
            window.clearInterval(window.generalInfoInterval);
        }
        let that = this;
        const { updateRunners } = this.props;
        this.fetchData(updateRunners)
        window.generalInfoInterval = setInterval(function () {
            that.fetchData(updateRunners)
        }, 15000)
    }
    render() {
        return (
            <Table
                columns={this.columns}
                rowKey={record => record.application_id}
                dataSource={this.state.data}
                // loading={this.state.loading}
                pagination={{ pageSize: 50 }}
                scroll={{ y: 400 }}
                size="middle"
            />
        );
    }
}

export { GeneralTable };