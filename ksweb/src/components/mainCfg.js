import React, { Component } from 'react'
import { Form, Input, } from 'antd';
import * as uuid from 'es6-uuid'

const FormItem = Form.Item;

class MainForm extends Component {
    constructor(props) {
        super(props);
        const { mainData, running } = this.props;
        this.state = {
            data: mainData,
            running: running,
            application_id: mainData.application_id !== undefined ? mainData.application_id : uuid(16),
        };
    };

    render() {
        const { data, running } = this.state;
        const { getFieldDecorator, } = this.props.form;
        const formItemLayout = {
            labelCol: {
                xs: { span: 24 },
                sm: { span: 7 },
            },
            wrapperCol: {
                xs: { span: 24 },
                sm: { span: 8 },
            },
        };
        return (
            <Form>
                <FormItem {...formItemLayout} label="ID">
                    {getFieldDecorator('application_id', {
                        initialValue: this.state.application_id,
                    })(<Input disabled placeholder='application.id' title='An identifier for the stream processing application.' />)}
                </FormItem>
                <FormItem {...formItemLayout} label="名称">
                    {getFieldDecorator('application_name', {
                        initialValue: data.application_name ? data.application_name : null,
                        rules: [{ required: true, message: '不能为空!' }],
                    })(<Input readOnly={running} placeholder='application.name' title='任务名称' />)}
                </FormItem>
                <FormItem {...formItemLayout} label="地址">
                    {getFieldDecorator('bootstrap_servers', {
                        initialValue: data.bootstrap_servers ? data.bootstrap_servers : null,
                        rules: [{ required: true, message: '不能为空!' }],
                    })(<Input readOnly={running} placeholder='bootstrap.servers' title='kafka address' />)}
                </FormItem>
                <FormItem {...formItemLayout} label="ZK地址">
                    {getFieldDecorator('ks_zookeeper_url', {
                        initialValue: data.ks_zookeeper_url ? data.ks_zookeeper_url : null,
                    })(<Input readOnly={running} placeholder='ks.zookeeper.url' title='zookeeper地址' />)}
                </FormItem>
                <FormItem {...formItemLayout} label="总缓存字节数">
                    {getFieldDecorator('cache_max_bytes_buffering', {
                        initialValue: data.cache_max_bytes_buffering ? data.cache_max_bytes_buffering : 10485760,
                        rules: [{ required: true, message: '不能为空!' }],
                    })(<Input readOnly={running} placeholder='cache.max.bytes.buffering' title='Maximum number of memory bytes to be used for buffering across all threads' />)}
                </FormItem>
                <FormItem {...formItemLayout} label="线程数">
                    {getFieldDecorator('num_stream_threads', {
                        initialValue: data.num_stream_threads ? data.num_stream_threads : 1,
                        rules: [{ required: true, message: '不能为空!' }],
                    })(<Input readOnly={running} placeholder='num.stream.threads' title='The number of threads to execute stream processing.' />)}
                </FormItem>
                <FormItem {...formItemLayout} label="各分区缓存记录">
                    {getFieldDecorator('buffered_records_per_partition', {
                        initialValue: data.buffered_records_per_partition ? data.buffered_records_per_partition : 1000,
                        rules: [{ required: true, message: '不能为空!' }],
                    })(<Input readOnly={running} placeholder='buffered.records.per.partition' title='The maximum number of records to buffer per partition.' />)}
                </FormItem>
                <FormItem {...formItemLayout} label="提交间隔">
                    {getFieldDecorator('commit_interval_ms', {
                        initialValue: data.commit_interval_ms ? data.commit_interval_ms : 30000,
                        rules: [{ required: true, message: '不能为空!' }],
                    })(<Input readOnly={running} placeholder='commit.interval.ms' title='The frequency with which to save the position of the processor.' />)}
                </FormItem>
                <FormItem {...formItemLayout} label="偏移策略">
                    {getFieldDecorator('auto_offset_reset', {
                        initialValue: data.auto_offset_reset ? data.auto_offset_reset : 'latest',
                        rules: [{ required: true, message: '不能为空!' }],
                    })(<Input readOnly={running} placeholder='auto.offset.reset' title='automatically reset the offset(earliest,latest)' />)}
                </FormItem>
            </Form>
        );
    }
}

export default Form.create()(MainForm);