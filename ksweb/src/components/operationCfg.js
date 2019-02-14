import React, { Component } from 'react'
import {
    Table, Icon, Button, Radio, Form, Input, Select, Modal,
    Popconfirm, notification, Row, Col,
} from 'antd';
import { DragDropContext, DragSource, DropTarget } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import update from 'immutability-helper';
// import * as uuid from 'es6-uuid';
const FormItem = Form.Item;
const Option = Select.Option;
const formItemLayout = {
    labelCol: {
        xs: { span: 24 },
        sm: { span: 8 },
    },
    wrapperCol: {
        xs: { span: 24 },
        sm: { span: 10 },
    },
};
const minFormItemLayout = {
    labelCol: {
        xs: { span: 12 },
        sm: { span: 10 },
    },
    wrapperCol: {
        xs: { span: 12 },
        sm: { span: 12 },
    },
};
function dragDirection(
    dragIndex,
    hoverIndex,
    initialClientOffset,
    clientOffset,
    sourceClientOffset,
) {
    const hoverMiddleY = (initialClientOffset.y - sourceClientOffset.y) / 2;
    const hoverClientY = clientOffset.y - sourceClientOffset.y;
    if (dragIndex < hoverIndex && hoverClientY > hoverMiddleY) {
        return 'downward';
    }
    if (dragIndex > hoverIndex && hoverClientY < hoverMiddleY) {
        return 'upward';
    }
};
let BodyRow = (props) => {
    const {
        isOver,
        connectDragSource,
        connectDropTarget,
        moveRow,
        dragRow,
        clientOffset,
        sourceClientOffset,
        initialClientOffset,
        ...restProps
    } = props;
    const style = { ...restProps.style, cursor: 'move' };
    let className = restProps.className;
    if (isOver && initialClientOffset) {
        const direction = dragDirection(
            dragRow.index,
            restProps.index,
            initialClientOffset,
            clientOffset,
            sourceClientOffset
        );
        if (direction === 'downward') {
            className += ' drop-over-downward';
        }
        if (direction === 'upward') {
            className += ' drop-over-upward';
        }
    }
    return connectDragSource(
        connectDropTarget(
            <tr
                {...restProps}
                className={className}
                style={style}
            />
        )
    );
};
const rowSource = {
    beginDrag(props) {
        return {
            index: props.index,
        };
    },
};
const rowTarget = {
    drop(props, monitor) {
        const dragIndex = monitor.getItem().index;
        const hoverIndex = props.index;
        // Don't replace items with themselves
        if (dragIndex === hoverIndex) {
            return;
        }
        // Time to actually perform the action
        props.moveRow(dragIndex, hoverIndex);
        // Note: we're mutating the monitor item here!
        // Generally it's better to avoid mutations,
        // but it's good here for the sake of performance
        // to avoid expensive index searches.
        monitor.getItem().index = hoverIndex;
    },
};
BodyRow = DropTarget('row', rowTarget, (connect, monitor) => ({
    connectDropTarget: connect.dropTarget(),
    isOver: monitor.isOver(),
    sourceClientOffset: monitor.getSourceClientOffset(),
}))(
    DragSource('row', rowSource, (connect, monitor) => ({
        connectDragSource: connect.dragSource(),
        dragRow: monitor.getItem(),
        clientOffset: monitor.getClientOffset(),
        initialClientOffset: monitor.getInitialClientOffset(),
    }))(BodyRow)
);

const ConvertKVForm = Form.create()(
    class extends Component {
        constructor(props) {
            super(props);
            this.state = {};
        };
        render() {
            const { data, running, form } = this.props;
            const { getFieldDecorator } = form;
            return (
                <Form>
                    <FormItem {...formItemLayout} label='加空值'>
                        {getFieldDecorator('kv_fields_noExist_append', { initialValue: data.kv_fields_noExist_append ? data.kv_fields_noExist_append : 'false' })(
                            <Radio.Group>
                                <Radio disabled={running} value="false">false</Radio>
                                <Radio disabled={running} value="true">true</Radio>
                            </Radio.Group>
                        )}
                    </FormItem>
                    <FormItem {...formItemLayout} label='新键'>
                        {getFieldDecorator('kv_key_fields', {
                            initialValue: data.kv_key_fields ? data.kv_key_fields : null,
                            // rules: [{ required: true, message: '不能为空!' }],
                        })(<Input readOnly={running} placeholder='kv.key.fields' title='新Key组成字段,与新值至少配置一项' />)}
                    </FormItem>
                    <FormItem {...formItemLayout} label='Key值格式'>
                        {getFieldDecorator('kv_key_fields_type', {
                            initialValue: data.kv_key_fields_type ? data.kv_key_fields_type : 'value',
                        })(<Radio.Group>
                            <Radio disabled={running} value="json">json</Radio>
                            <Radio disabled={running} value="value">value</Radio>
                        </Radio.Group>)}
                    </FormItem>
                    <FormItem {...formItemLayout} label='新值'>
                        {getFieldDecorator('kv_value_fields', {
                            initialValue: data.kv_value_fields ? data.kv_value_fields : null,
                            // rules: [{ required: true, message: '不能为空!' }],
                        })(<Input readOnly={running} placeholder='kv.value.fields' title='新Value组成字段,与新键至少配置一项' />)}
                    </FormItem>
                </Form>
            );
        };
    }
);
const ConvertTimeForm = Form.create()(
    class extends Component {
        constructor(props) {
            super(props);
            this.state = {};
        };
        render() {
            const { data, running, form } = this.props;
            const { getFieldDecorator } = form;
            return (
                <Form>
                    <Row gutter={24}>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输入字段'>
                                {getFieldDecorator('time_in_names', {
                                    initialValue: data.time_in_names ? data.time_in_names : null,
                                    rules: [{ required: true, message: '不能为空!' }],
                                })(<Input readOnly={running} placeholder='time.in.names' title='输入时间字段.如:f1,f2,f3...' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输入字段类型'>
                                {getFieldDecorator('time_in_value_types', {
                                    initialValue: data.time_in_value_types ? data.time_in_value_types : null,
                                    rules: [{ required: true, message: '不能为空!' }],
                                })(<Input readOnly={running} placeholder='time.in.value.types' title='时间值类型[long,string]与输入字段对应' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输出字段'>
                                {getFieldDecorator('time_out_names', {
                                    initialValue: data.time_out_names ? data.time_out_names : null,
                                })(<Input readOnly={running} placeholder='time.out.names' title='输出时间字段.如:o1,o2,o3...与输入字段对应.不配置即默认输入字段' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输入字段格式'>
                                {getFieldDecorator('time_in_formats', {
                                    initialValue: data.time_in_formats ? data.time_in_formats : null,
                                })(<Input readOnly={running} placeholder='time.in.formats' title='long值无需配置format,而long,string同时存在则format对应的long要添加半角逗号如:[long,string]==>[,uuuu-MM-dd]' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输出字段格式'>
                                {getFieldDecorator('time_out_formats', {
                                    initialValue: data.time_out_formats ? data.time_out_formats : null,
                                })(<Input readOnly={running} placeholder='time.out.formats' title='如果只配置单一值,则统一输出此格式;如果不配置则输出unix时间戳' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输入格式环境'>
                                {getFieldDecorator('time_in_lang', {
                                    initialValue: data.time_in_lang ? data.time_in_lang : null,
                                })(<Input readOnly={running} placeholder='默认en' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输出格式环境'>
                                {getFieldDecorator('time_out_lang', {
                                    initialValue: data.time_out_lang ? data.time_out_lang : null,
                                })(<Input readOnly={running} placeholder='默认en' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输入字段时区'>
                                {getFieldDecorator('time_in_offsetId', {
                                    initialValue: data.time_in_offsetId ? data.time_in_offsetId : null,
                                })(<Input readOnly={running} placeholder='time.in.offsetId' title="默认东八区(+08:00)如果值类似yyyy-MM-dd'T'HH:mm:ss.SSSZ,则需配置为零时区(+00:00)" />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='输出字段时区'>
                                {getFieldDecorator('time_out_offsetId', {
                                    initialValue: data.time_out_offsetId ? data.time_out_offsetId : null,
                                })(<Input readOnly={running} placeholder='time.out.offsetId' title="默认东八区(+08:00)如果值类似yyyy-MM-dd'T'HH:mm:ss.SSSZ,则需配置为零时区(+00:00)" />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='异常输出'>
                                {getFieldDecorator('time_error_out', {
                                    initialValue: data.time_error_out ? data.time_error_out : null,
                                })(<Input readOnly={running} placeholder='time.error.out' title='出现异常输出,未配置则会中断运行' />)}
                            </FormItem>
                        </Col>
                    </Row>
                </Form>
            );
        };
    }
);
const WindowForm = Form.create()(
    class extends Component {
        constructor(props) {
            super(props);
            this.state = {};
        };
        render() {
            const { data, running, form } = this.props;
            const { getFieldDecorator } = form;
            return (
                <Form>
                    <FormItem {...formItemLayout} label='值追加重复'>
                        {getFieldDecorator('window_uncover_field_repeat', {
                            initialValue: data.window_uncover_field_repeat ? data.window_uncover_field_repeat : 'false',
                        })(<Radio.Group>
                            <Radio disabled={running} value="false">false</Radio>
                            <Radio disabled={running} value="true">true</Radio>
                        </Radio.Group>)}
                    </FormItem>
                    <Row gutter={24}>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='窗口大小'>
                                {getFieldDecorator('window_sizeMs', {
                                    initialValue: data.window_sizeMs ? data.window_sizeMs : '600000',
                                    rules: [{ required: true, message: '不能为空!' }],
                                })(<Input readOnly={running} placeholder='window.sizeMs' title='The size of the windows in milliseconds' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='窗口间隔'>
                                {getFieldDecorator('window_advanceMs', {
                                    initialValue: data.window_advanceMs ? data.window_advanceMs : '600000',
                                    rules: [{ required: true, message: '不能为空!' }],
                                })(<Input readOnly={running} placeholder='window.advanceMs' title='The size of the window advance interval in milliseconds' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='窗口期限'>
                                {getFieldDecorator('window_retentionMs', {
                                    initialValue: data.window_retentionMs ? data.window_retentionMs : '600000',
                                    rules: [{ required: true, message: '不能为空!' }],
                                })(<Input readOnly={running} placeholder='window.retentionMs' title='a guaranteed lower bound for how long a window will be maintained' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='统计字段'>
                                {getFieldDecorator('window_count', {
                                    initialValue: data.window_count ? data.window_count : null,
                                    rules: [{ required: true, message: '不能为空!' }],
                                })(<Input readOnly={running} placeholder='window.count' title='聚合运算后输出的统计字段,注意输出字段的配置' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='开始时间'>
                                {getFieldDecorator('window_startTime', {
                                    initialValue: data.window_startTime ? data.window_startTime : null,
                                })(<Input readOnly={running} placeholder='window.startTime' title='记录的窗口开始时间,注意输出字段的配置' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='结束时间'>
                                {getFieldDecorator('window_endTime', {
                                    initialValue: data.window_endTime ? data.window_endTime : null,
                                })(<Input readOnly={running} placeholder='window.endTime' title='记录的窗口结束时间,注意输出字段的配置' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='缓存名称'>
                                {getFieldDecorator(' window_store_name', {
                                    initialValue: data.window_store_name ? data.window_store_name : 'aggregation',
                                })(<Input readOnly={running} placeholder=' window.store.name' title='有多个window操作则需要配置不同的名称' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='值追加字段'>
                                {getFieldDecorator('window_uncover_fields', {
                                    initialValue: data.window_uncover_fields ? data.window_uncover_fields : null,
                                })(<Input readOnly={running} placeholder='window.uncover.fields' title='不覆盖的字段(int自动求和,string尾部追加).如:f1,f2,f3...' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='值追加间隔符'>
                                {getFieldDecorator('window_uncover_field_interval', {
                                    initialValue: data.window_uncover_field_interval ? data.window_uncover_field_interval : ',',
                                })(<Input readOnly={running} placeholder='window.uncover.field.interval' title="不覆盖的field间隔符.默认','" />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='隔离符'>
                                {getFieldDecorator('window_uncover_field_prefix', {
                                    initialValue: data.window_uncover_field_prefix ? data.window_uncover_field_prefix : '__',
                                })(<Input readOnly={running} placeholder='window.uncover.field.prefix' title='默认__,追加字段名称含有这个符号则修改' />)}
                            </FormItem>
                        </Col>
                    </Row>
                </Form>
            );
        };
    }
);
const JoinForm = Form.create()(
    class extends Component {
        constructor(props) {
            super(props);
            this.state = {};
        };
        render() {
            const { data, running, form } = this.props;
            const { getFieldDecorator } = form;
            return (
                <Form>
                    <FormItem {...formItemLayout} label='值是否覆盖'>
                        {getFieldDecorator('join_output_strategy', {
                            initialValue: data.join_output_strategy ? data.join_output_strategy : 'cover',
                        })(<Radio.Group>
                            <Radio disabled={running} value="cover">cover</Radio>
                            <Radio disabled={running} value="uncover">uncover</Radio>
                        </Radio.Group>)}
                    </FormItem>
                    <Row gutter={24}>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='自定义Store'>
                                {getFieldDecorator('operation_table_store', {
                                    initialValue: data.operation_table_store ? data.operation_table_store : null,
                                })(<Input readOnly={running} placeholder='operation.table.store' title='table对table时建议配置' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='不覆盖值字段'>
                                {getFieldDecorator('join_output_fields_value_add', {
                                    initialValue: data.join_output_fields_value_add ? data.join_output_fields_value_add : null,
                                })(<Input readOnly={running} placeholder='join.output.fields.value.add' title='输出策略外的允许同名字段值追加,优先级高于输出策略,半角逗号分隔' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='不覆盖值间隔符'>
                                {getFieldDecorator('join_output_fields_value_add_interval', {
                                    initialValue: data.join_output_fields_value_add_interval ? data.join_output_fields_value_add_interval : ',',
                                })(<Input readOnly={running} placeholder='join.output.fields.value.add.interval' title=' 值追加间隔符' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='转接源'>
                                {getFieldDecorator('join_source_through', {
                                    initialValue: data.join_source_through ? data.join_source_through : null,
                                })(<Input readOnly={running} placeholder='join.source.through' title='source的partition与target不一致,运行前手动创建一个与target相同partition的topic,默认分区方式' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='转接目标源'>
                                {getFieldDecorator('join_target_through', {
                                    initialValue: data.join_target_through ? data.join_target_through : null,
                                })(<Input readOnly={running} placeholder='join.target.through' title='target的partition与source不一致,运行前手动创建一个与source相同partition的topic,默认分区方式' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='beforeMs'>
                                {getFieldDecorator('join_beforeMs', {
                                    initialValue: data.join_beforeMs ? data.join_beforeMs : 0,
                                })(<Input readOnly={running} placeholder='join.beforeMs' title='非stream对stream,无需配置' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='afterMs'>
                                {getFieldDecorator('join_afterMs', {
                                    initialValue: data.join_afterMs ? data.join_afterMs : 0,
                                })(<Input readOnly={running} placeholder='join.afterMs' title='非stream对stream,无需配置' />)}
                            </FormItem>
                        </Col>
                        <Col span={12} style={{ padding: 0 }}>
                            <FormItem {...minFormItemLayout} label='retentionMs'>
                                {getFieldDecorator('join_retentionMs', {
                                    initialValue: data.join_retentionMs ? data.join_retentionMs : 1,
                                })(<Input readOnly={running} placeholder='join.retentionMs' title='非stream对stream,无需配置' />)}
                            </FormItem>
                        </Col>
                    </Row>
                </Form>
            );
        };
    }
);
const OperationForm = Form.create()(
    class extends Component {
        constructor(props) {
            super(props);
            const { data } = this.props;
            this.state = {
                operator: data.operator ? data.operator : 'convertKV',
            };
        };
        changeOperator = (value) => {
            this.setState({ operator: value });
        };
        convertKVFormRef = (formRef) => {
            this.convertKVRef = formRef;
        };
        convertTimeFormRef = (formRef) => {
            this.convertTimeRef = formRef;
        };
        windowFormRef = (formRef) => {
            this.windowRef = formRef;
        };
        joinFormRef = (formRef) => {
            this.joinRef = formRef;
        };
        onOk = () => {
            const { onUpdate } = this.props;
            let form = this.convertKVRef.props.form;
            switch (this.state.operator) {
                case 'convertKV':
                    form = this.convertKVRef.props.form;
                    break;
                case 'convertTime':
                    form = this.convertTimeRef.props.form;
                    break;
                case 'window':
                    form = this.windowRef.props.form;
                    break;
                case 'join':
                case 'leftJoin':
                case 'outerJoin':
                    form = this.joinRef.props.form;
                    break;
                default:
            }
            form.validateFields((err, values) => {
                if (err) {
                    notification.warning({ message: "表单校验未通过,请检查!", duration: 5, })
                } else {
                    // message.info(JSON.stringify(values));
                    this.convertKVRef.props.form.resetFields();
                    this.convertTimeRef.props.form.resetFields();
                    this.windowRef.props.form.resetFields();
                    this.joinRef.props.form.resetFields();
                    onUpdate(values);
                }
            });
        };
        render() {
            const { visible, data, running, onCancel, form } = this.props;
            const { getFieldDecorator } = form;
            const { operator } = this.state;
            return (
                <Modal
                    visible={visible}
                    title={data.operation_name ? data.operation_name : '新增数据源'}
                    okText='确定'
                    cancelText='取消'
                    onCancel={onCancel}
                    onOk={this.onOk}>
                    <Form>
                        {/* <FormItem {...formItemLayout} label='操作ID' style={{ display: 'none' }}>
                            {getFieldDecorator('operation_id', { initialValue: data.operation_id, })(<Input />)}
                        </FormItem> */}
                        <FormItem {...formItemLayout} label='操作名称'>
                            {getFieldDecorator('operation_name', {
                                initialValue: data.operation_name ? data.operation_name : null,
                                rules: [{ required: true, message: '不能为空!' }],
                            })(<Input readOnly={running} placeholder='operation.name' title='执行什么操作' />)}
                        </FormItem>
                        <FormItem {...formItemLayout} label='数据源'>
                            {getFieldDecorator('operation_ks_name', {
                                initialValue: data.operation_ks_name ? data.operation_ks_name : null,
                                rules: [{ required: true, message: '不能为空!' }],
                            })(<Input readOnly={running} placeholder='operation.ks.name' title='执行操作的kSource名称' />)}
                        </FormItem>
                        <FormItem {...formItemLayout} label='目标源'>
                            {getFieldDecorator('join_ks_name', {
                                initialValue: data.join_ks_name ? data.join_ks_name : null,
                            })(<Input readOnly={running} placeholder='join.ks.name' title='执行操作的目标kSource名称,如join操作' />)}
                        </FormItem>
                        {/* <FormItem {...formItemLayout} label='自定义Store'>
                            {getFieldDecorator('operation_table_store', {
                                initialValue: data.operation_table_store ? data.operation_table_store : null,
                            })(<Input readOnly={running} placeholder='kSource是table提供自定义的storeName' />)}
                        </FormItem> */}
                        <FormItem {...formItemLayout} label='操作'>
                            {getFieldDecorator('operation_operator', {
                                initialValue: data.operation_operator ? data.operation_operator : 'convertKV',
                            })(<Select onChange={this.changeOperator}>
                                <Option disabled={running} value="convertKV">键值重组</Option>
                                <Option disabled={running} value="convertTime">时间转换</Option>
                                <Option disabled={running} value="window" >窗口聚合</Option>
                                <Option disabled={running} value="join">连接</Option>
                                <Option disabled={running} value="leftJoin">左连</Option>
                                <Option disabled={running} value="outerJoin">外连</Option>
                            </Select>)}
                        </FormItem>
                    </Form>
                    <div className={operator === 'convertKV' ? 'show-div' : 'hide-div'}>
                        <ConvertKVForm wrappedComponentRef={this.convertKVFormRef}
                            data={data} running={running} />
                    </div>
                    <div className={operator === 'convertTime' ? 'show-div' : 'hide-div'}>
                        <ConvertTimeForm wrappedComponentRef={this.convertTimeFormRef}
                            data={data} running={running} />
                    </div>
                    <div className={operator === 'window' ? 'show-div' : 'hide-div'}>
                        <WindowForm wrappedComponentRef={this.windowFormRef}
                            data={data} running={running} />
                    </div>
                    <div className={operator.toLowerCase().includes('join') ? 'show-div' : 'hide-div'}>
                        <JoinForm wrappedComponentRef={this.joinFormRef}
                            data={data} running={running} />
                    </div>
                </Modal>
            );
        };
    }
);

class OperationTable extends Component {
    constructor(props) {
        super(props);
        const { operationsData, running } = this.props;
        this.state = {
            data: operationsData,
            running: running,
            modalVisable: false,
            modalData: {},
        };
        this.columns = [{
            title: '操作名称',
            dataIndex: 'operation_name',
            width: '20%',
        }, {
            title: '数据源',
            dataIndex: 'operation_ks_name',
            width: '15%',
        }, {
            title: '目标源',
            dataIndex: 'join_ks_name',
            width: '15%',
        }, {
            title: '操作',
            dataIndex: 'operation_operator',
            width: '15%',
        }, {
            title: '更多',
            dataIndex: 'handle',
            width: '15%',
            render: (text, record) => {
                return (
                    <Button type="primary" ghost onClick={() => this.showModal(record)}><Icon type="file-text" /></Button>
                );
            }
        }, {
            title: '删除',
            dataIndex: 'delete',
            width: '15%',
            render: (text, record) => {
                return this.state.running ? null : (
                    <Popconfirm title="确认删除吗?" onConfirm={() => this.deleteOneTask(record.operation_name)}>
                        <Icon type="delete" style={{ cursor: 'pointer', fontSize: 16, color: '#08c' }} /></Popconfirm>
                );
            }
        }];
    }
    components = {
        body: {
            row: BodyRow,
        },
    };

    moveRow = (dragIndex, hoverIndex) => {
        const { data } = this.state;
        const { updateData } = this.props;
        const dragRow = data[dragIndex];
        // message.info(JSON.stringify(data));
        let sortData = update(this.state, {
            data: {
                $splice: [[dragIndex, 1], [hoverIndex, 0, dragRow]],
            },
        });
        this.setState(sortData);
        updateData('operations', sortData);
        // message.info(dragIndex + '====' + hoverIndex);
    };
    deleteOneTask = (operation_name) => {
        const dataSource = [...this.state.data];
        const { updateData } = this.props;
        let newData = dataSource.filter(item => item.operation_name !== operation_name);
        this.setState({ data: newData });
        updateData('operations', newData);
        notification.success({ message: "删除成功", duration: 1, })
    };
    showModal = (source) => {
        let data = source !== undefined ? source : {};// { operation_id: uuid(16) };
        // message.info(JSON.stringify(data));
        this.setState({ modalVisable: true, modalData: data, });
    };
    handleCancel = () => {
        this.setState({ modalVisable: false, });
    };
    updateSource = (childValues) => {
        const form = this.formRef.props.form;
        const { updateData } = this.props;
        form.validateFields((err, values) => {
            if (err) {
                notification.warning({ message: "表单校验未通过,请检查!", duration: 5, })
            } else {
                form.resetFields();
                let newData = this.state.data.filter((item) => {
                    return this.state.modalData.operation_name !== item.operation_name
                })
                Object.assign(values, childValues);
                // message.info(JSON.stringify(values));
                newData.push(values);
                this.setState({ modalVisable: false, modalData: {}, data: newData });
                updateData('operations', newData);
            }
        });
    };
    saveFormRef = (formRef) => {
        this.formRef = formRef;
    };
    render() {
        // message.info(running.toString());
        return (
            <div id='operationCfg'>
                <Row gutter={24} style={{ marginBottom: '6px', }}>
                    <Col span={3} offset={21}>
                        <Button type="primary" ghost icon="plus" disabled={this.state.running} onClick={() => this.showModal()}> 添加</Button>
                    </Col>
                </Row>
                <Table
                    columns={this.columns}
                    rowKey={record => record.operation_name}
                    dataSource={this.state.data}
                    components={this.components}
                    onRow={(record, index) => ({
                        index,
                        moveRow: this.moveRow,
                    })}
                    // loading={this.state.loading}
                    pagination={{ pageSize: 50 }}
                    scroll={{ y: 400 }}
                    size="middle"
                />
                <OperationForm
                    wrappedComponentRef={this.saveFormRef}
                    visible={this.state.modalVisable}
                    data={this.state.modalData}
                    running={this.state.running}
                    onCancel={this.handleCancel}
                    onUpdate={this.updateSource}
                />
            </div>
        );
    };
};
const OperationCfg = DragDropContext(HTML5Backend)(OperationTable);

export { OperationCfg };