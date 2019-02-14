import React, { Component } from 'react'
import { Form, Input, Select, Row, Col, Radio } from 'antd';
import * as uuid from 'es6-uuid'

const FormItem = Form.Item;
const Option = Select.Option;
const formItemLayout = {
    labelCol: {
        xs: { span: 12 },
        sm: { span: 8 },
    },
    wrapperCol: {
        xs: { span: 12 },
        sm: { span: 12 },
    },
};
class OutputForm extends Component {
    constructor(props) {
        super(props);
        const { outputData, running } = this.props;
        this.state = {
            data: outputData,
            running: running,
            // output_id: outputData.output_id !== undefined ? outputData.output_id : uuid(16),
        };
    };

    render() {
        const { data, running } = this.state;
        const { getFieldDecorator, setFieldsValue } = this.props.form;
        return (
            <Form>
                <Row gutter={24}>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="输出源名称">
                            {getFieldDecorator('output_ks_name', {
                                initialValue: data.output_ks_name ? data.output_ks_name : null,
                                rules: [{ required: true, message: '不能为空!' }],
                            })(<Input readOnly={running} placeholder='output.ks.name' title='输出经过一系列处理后的kSource' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="输出字段">
                            {getFieldDecorator('output_fields', {
                                initialValue: data.output_fields ? data.output_fields : null,
                            })(<Input readOnly={running} placeholder='output.fields' title='输出字段,如:f1,f2,f3不配置默认所有' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="输出目标">
                            {getFieldDecorator('output_targets', {
                                initialValue: data.output_targets ? data.output_targets.split(',') : ['sysout'],
                                rules: [{ required: true, message: '不能为空!' }],
                            })(<Select mode="multiple">
                                <Option disabled={running} value="sysout">sysout</Option>
                                <Option disabled={running} value="kafka">kafka</Option>
                                <Option disabled={running} value="zbus">zbus</Option>
                            </Select>)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="kafka地址">
                            {getFieldDecorator('output_target_kafka_address', {
                                initialValue: data.output_target_kafka_address ? data.output_target_kafka_address : null,
                            })(<Input readOnly={running} placeholder='output.target.kafka.address' title='输出的kafka地址,默认使用流地址' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="kafka主题">
                            {getFieldDecorator('output_target_kafka_topic', {
                                initialValue: data.output_target_kafka_topic ? data.output_target_kafka_topic : null,
                            })(<Input readOnly={running} placeholder='output.target.kafka.topic' title='输出的kafka主题' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="zbus地址">
                            {getFieldDecorator('output_target_zbus_address', {
                                initialValue: data.output_target_zbus_address ? data.output_target_zbus_address : null,
                            })(<Input readOnly={running} placeholder='output.target.zbus.address' title='输出的zbus地址' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="zbus队列">
                            {getFieldDecorator('output_target_zbus_mq', {
                                initialValue: data.output_target_zbus_mq ? data.output_target_zbus_mq : null,
                            })(<Input readOnly={running} placeholder='output.target.zbus.mq' title='输出的zbus队列' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="窗口过滤">
                            {getFieldDecorator('expandWin_enable', {
                                initialValue: data.expandWin_enable ? data.expandWin_enable : 'false',
                            })(
                                <Radio.Group onChange={(e) =>
                                    e.target.value === 'true' ?
                                        setFieldsValue({ 'expandWin_store_name': uuid(16) })
                                        : setFieldsValue({ 'expandWin_store_name': '' })
                                }>
                                    <Radio disabled={running} value="false">false</Radio>
                                    <Radio disabled={running} value="true">true</Radio>
                                </Radio.Group>
                            )}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="窗口大小">
                            {getFieldDecorator('expandWin_expireTime', {
                                initialValue: data.expandWin_expireTime ? data.expandWin_expireTime : null,
                            })(<Input readOnly={running} placeholder='expandWin.expireTime' title='窗口大小单位秒' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="线程数">
                            {getFieldDecorator('expandWin_background_threads', {
                                initialValue: data.expandWin_background_threads ? data.expandWin_background_threads : 1,
                            })(<Input readOnly={running} placeholder='expandWin.background.threads' title='窗口处理线程数' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="调度周期">
                            {getFieldDecorator('expandWin_executorPeriod', {
                                initialValue: data.expandWin_executorPeriod ? data.expandWin_executorPeriod : 10000,
                            })(<Input readOnly={running} placeholder='expandWin.executorPeriod' title='检查窗口窗台间隔单位毫秒' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="过滤数">
                            {getFieldDecorator('expandWin_countFiled', {
                                initialValue: data.expandWin_countFiled ? data.expandWin_countFiled : null,
                            })(<Input readOnly={running} placeholder='expandWin.countFiled' title='过滤的记录数,注意输出字段的配置' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="自定义输出">
                            {getFieldDecorator('format_enable', {
                                initialValue: data.format_enable ? data.format_enable : 'false',
                            })(
                                <Radio.Group>
                                    <Radio disabled={running} value="false">false</Radio>
                                    <Radio disabled={running} value="true">true</Radio>
                                </Radio.Group>
                            )}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="自定义格式">
                            {getFieldDecorator('format_pattern', {
                                initialValue: data.format_pattern ? data.format_pattern : null,
                            })(<Input readOnly={running} placeholder='format.pattern' title='变量前后加上$且变量名不能含有双引号如:{"columns":["数量"],"index":["$window_start$"]}' />)}
                        </FormItem>
                    </Col>
                    <Col span={12} style={{ padding: 0 }}>
                        <FormItem {...formItemLayout} label="内存标识">
                            {getFieldDecorator('expandWin_store_name', {
                                // initialValue: this.state.output_id,
                            })(<Input disabled placeholder='expandWin.store.name' title='窗口内存记录别名' />)}
                        </FormItem>
                    </Col>
                </Row>
            </Form>
        );
    }
}

export default Form.create()(OutputForm);