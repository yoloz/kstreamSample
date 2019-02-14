import React, { Component } from 'react'
import {
    Table, Icon, Button, Radio, Form, Input, Modal,
    Popconfirm, notification, Row, Col
} from 'antd';

const FormItem = Form.Item;

const ModalForm = Form.create()(
    class extends Component {
        render() {
            const { visible, data, running, onCancel, onUpdate, form } = this.props;
            const { getFieldDecorator } = form;
            const formItemLayout = {
                labelCol: {
                    xs: { span: 24 },
                    sm: { span: 8 },
                },
                wrapperCol: {
                    xs: { span: 24 },
                    sm: { span: 12 },
                },
            };
            return (
                <Modal
                    visible={visible}
                    title={data.ks_name ? data.ks_name : '新增数据源'}
                    okText='确定'
                    cancelText='取消'
                    onCancel={onCancel}
                    onOk={onUpdate}>
                    <Form>
                        <FormItem {...formItemLayout} label='名称'>
                            {getFieldDecorator('ks_name', {
                                initialValue: data.ks_name ? data.ks_name : null,
                                rules: [{ required: true, message: '不能为空!' }],
                            })(<Input readOnly={running} placeholder='ks.name' title='kSource名称,一般同主题名' />)}
                        </FormItem>
                        <FormItem {...formItemLayout} label='类型'>
                            {getFieldDecorator('ks_type', { initialValue: data.ks_type ? data.ks_type : 'stream' })(
                                <Radio.Group>
                                    <Radio disabled={running} value="stream">stream</Radio>
                                    <Radio disabled={running} value="table">table</Radio>
                                </Radio.Group>
                            )}
                        </FormItem>
                        <FormItem {...formItemLayout} label='主题'>
                            {getFieldDecorator('ks_topics', {
                                initialValue: data.ks_topics ? data.ks_topics : null,
                                rules: [{ required: true, message: '不能为空!' }],
                            })(<Input readOnly={running} placeholder='ks.topics' title='kafka topic' />)}
                        </FormItem>
                        {/* <FormItem {...formItemLayout} label='自定义Store'>
                            {getFieldDecorator('ks_table_store', {
                                initialValue: data.ks_table_store ? data.ks_table_store : null,
                            })(<Input readOnly={running} placeholder='ks.table.store' title='type是table时提供自定义的storeName' />)}
                        </FormItem> */}
                        <FormItem {...formItemLayout} label='时间字段'>
                            {getFieldDecorator('ks_time_name', {
                                initialValue: data.ks_time_name ? data.ks_time_name : null,
                            })(<Input readOnly={running} placeholder='ks.time.name' title='事件时间字段,默认kafka record的timestamp' />)}
                        </FormItem>
                        <FormItem {...formItemLayout} label='时间值类型'>
                            {getFieldDecorator('ks_time_type', { initialValue: data.ks_time_type ? data.ks_time_type : 'long' })(
                                <Radio.Group>
                                    <Radio disabled={running} value="long">long</Radio>
                                    <Radio disabled={running} value="string">string</Radio>
                                </Radio.Group>
                            )}
                        </FormItem>
                        <FormItem {...formItemLayout} label='时间值格式'>
                            {getFieldDecorator('ks_time_format', {
                                initialValue: data.ks_time_format ? data.ks_time_format : null,
                            })(<Input readOnly={running} placeholder='ks.time.format' title='时间值类型string需要配置字符串格式' />)}
                        </FormItem>
                        <FormItem {...formItemLayout} label='时间环境'>
                            {getFieldDecorator('ks_time_lang', {
                                initialValue: data.ks_time_lang ? data.ks_time_lang : null,
                            })(<Input readOnly={running} placeholder='默认en' />)}
                        </FormItem>
                        <FormItem {...formItemLayout} label='时区'>
                            {getFieldDecorator('ks_time_offsetId', {
                                initialValue: data.ks_time_offsetId ? data.ks_time_offsetId : null,
                            })(<Input readOnly={running} placeholder='ks.time.offsetId' title="默认东八区(+08:00)如果值类似yyyy-MM-dd'T'HH:mm:ss.SSSZ,则需配置为零时区(+00:00)" />)}
                        </FormItem>
                    </Form>
                </Modal>
            );
        };
    }
);

class SourceCfg extends Component {
    constructor(props) {
        super(props);
        const { sourcesData, running } = this.props;
        this.state = {
            data: sourcesData,
            running: running,
            modalVisable: false,
            modalData: {},
        };
        this.columns = [{
            title: '名称',
            dataIndex: 'ks_name',
            width: '20%',
        }, {
            title: '类型',
            dataIndex: 'ks_type',
            width: '20%',
        }, {
            title: '主题',
            dataIndex: 'ks_topics',
            width: '20%',
            // }, {
            //     title: '时间字段',
            //     dataIndex: 'tname',
            //     width: '20%',
        }, {
            title: '操作',
            dataIndex: 'handle',
            width: '20%',
            render: (text, record) => {
                return (
                    <Button type="primary" ghost onClick={() => this.showModal(record)}><Icon type="file-text" /></Button>
                );
            }
        }, {
            title: '删除',
            dataIndex: 'delete',
            width: '20%',
            render: (text, record) => {
                return this.state.running ? null : (
                    <Popconfirm title="确认删除吗?" onConfirm={() => this.deleteOneTask(record.ks_topics)}>
                        <Icon type="delete" style={{ cursor: 'pointer', fontSize: 16, color: '#08c' }} /></Popconfirm>
                );
            }
        }];
    }
    deleteOneTask = (ks_topics) => {
        const dataSource = [...this.state.data];
        const { updateData } = this.props;
        let newData = dataSource.filter(item => item.ks_topics !== ks_topics);
        this.setState({ data: newData });
        updateData('sources', newData);
        notification.success({ message: "删除成功", duration: 1, })
    };
    showModal = (source) => {
        let data = source ? source : {};
        this.setState({ modalVisable: true, modalData: data, });
    };
    handleCancel = () => {
        this.setState({ modalVisable: false, });
    };
    updateSource = () => {
        const form = this.formRef.props.form;
        const { updateData } = this.props;
        form.validateFields((err, values) => {
            if (err) {
                notification.warning({ message: "表单校验未通过,请检查!", duration: 5, })
            } else {
                // message.info(JSON.stringify(values));
                form.resetFields();
                let newData = this.state.data.filter((item) => {
                    return this.state.modalData.ks_topics !== item.ks_topics
                })
                newData.push(values);
                this.setState({ modalVisable: false, modalData: {}, data: newData });
                updateData('sources', newData);
            }
        });
    };
    saveFormRef = (formRef) => {
        this.formRef = formRef;
    };
    render() {
        const { data, modalData, modalVisable, running } = this.state;
        // message.info(JSON.stringify(data));
        return (
            <div>
                <Row gutter={24} style={{ marginBottom: '6px', }}>
                    <Col span={3} offset={21}>
                        <Button type="primary" ghost icon="plus" disabled={running} onClick={() => this.showModal()}> 添加</Button>
                    </Col>
                </Row>
                <Table
                    columns={this.columns}
                    rowKey={record => record.ks_topics}
                    dataSource={data}
                    // loading={this.state.loading}
                    // pagination={{ pageSize: 50 }}
                    // scroll={{ y: 400 }}
                    size="middle"
                />
                <ModalForm
                    wrappedComponentRef={this.saveFormRef}
                    visible={modalVisable}
                    data={modalData}
                    running={running}
                    onCancel={this.handleCancel}
                    onUpdate={this.updateSource}
                />
            </div>
        );
    };
};

export { SourceCfg };