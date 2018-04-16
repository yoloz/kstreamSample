import React, { Component } from 'react';
import { Steps, Button, notification, } from 'antd';
import { storeTask } from '../services/restApi'
import MainForm from '../components/mainCfg'
import { SourceCfg } from '../components/sourceCfg'
import { OperationCfg } from '../components/operationCfg'
import OutputForm from '../components/outputCfg'

const Step = Steps.Step;

const steps = [{
    title: 'KS参数',
    content: 'main',
}, {
    title: '数据源',
    content: 'source',
}, {
    title: '操作步骤',
    content: 'operations',
}, {
    title: '输出',
    content: 'output',
}];

class NewTaskPage extends Component {
    constructor(props) {
        super(props);
        const { defaultStep, defaultData, running } = this.props;
        this.state = {
            running: running,
            // 0,1,2
            stepIndex: defaultStep,
            // {main:{}, sources: [], operations: [], output: {} }
            stepData: defaultData,
        };
    };
    updateData(type, obj) {
        let data = this.state.stepData;
        switch (type) {
            case 'main':
                data.main = obj;
                break;
            case 'sources':
                data.sources = obj;
                break;
            case 'operations':
                data.operations = obj;
                break;
            case 'output':
                data.output = obj;
                break;
            default:
        }
        this.setState({ stepData: data, });
    };
    storeData() {
        const { backToCfgMgr } = this.props;
        this.refs.checkOutputForm.validateFields((err, values) => {
            if (err) {
                notification.warning({ message: "表单校验未通过,请检查!", duration: 5, })
            } else {
                // const current = this.state.stepIndex + 1;
                this.updateData('output', values);
                // this.setState({ stepIndex: current, });
                // message.info("3=====" + JSON.stringify(this.state.stepData));
                storeTask(this.state.stepData).then((data) => {
                    if (data.success) {
                        backToCfgMgr();
                        // } else {
                        //     notification['error']({
                        //         message: '保存失败!',
                        //         description: data.toString(),
                        //         duration: 1.5
                        //     });
                    } else {
                        notification['error']({
                            message: '异常',
                            description: data.error,
                            duration: 1.5
                        });
                    }
                });
            }
        });
    };
    next() {
        if (this.state.stepIndex === 0) {
            this.refs.checkMainForm.validateFields((err, values) => {
                if (err) {
                    notification.warning({ message: "表单校验未通过,请检查!", duration: 5, })
                } else {
                    const current = this.state.stepIndex + 1;
                    this.updateData('main', values);
                    this.setState({ stepIndex: current, });
                    // message.info("0=====" + JSON.stringify(this.state.stepData));
                }
            });
        } else if (this.state.stepIndex === 1) {
            const current = this.state.stepIndex + 1;
            this.setState({ stepIndex: current, });
            // message.info("1=====" + JSON.stringify(this.state.stepData));
        } else if (this.state.stepIndex === 2) {
            const current = this.state.stepIndex + 1;
            this.setState({ stepIndex: current, });
            // message.info("2=====" + JSON.stringify(this.state.stepData));
        }
    };
    prev() {
        const stepIndex = this.state.stepIndex - 1;
        this.setState({ stepIndex });
    };
    render() {
        const { running, stepIndex, stepData } = this.state;
        return (
            <div>
                <Steps current={stepIndex}>
                    {steps.map(item => <Step key={item.title} title={item.title} />)}
                </Steps>
                <div className="steps-content" style={{ marginBottom: 10 + 'px' }}>
                    {/* {steps[stepIndex].content} */}
                    {/* <div><p className={stepIndex <= 3 ? 'show-div info' : 'hide-div'}>注意：请根据实际情况修改，有默认值的可以直接使用默认值</p></div> */}
                    <div className={stepIndex === 0 ? 'show-div' : 'hide-div'}>
                        <MainForm ref='checkMainForm' mainData={stepData.main} running={running} />
                    </div>
                    <div className={stepIndex === 1 ? 'show-div' : 'hide-div'}>
                        <SourceCfg updateData={this.updateData.bind(this)} sourcesData={stepData.sources} running={running} />
                    </div>
                    <div className={stepIndex === 2 ? 'show-div' : 'hide-div'}>
                        <OperationCfg updateData={this.updateData.bind(this)} operationsData={stepData.operations} running={running} />
                    </div>
                    <div className={stepIndex === 3 ? 'show-div' : 'hide-div'}>
                        <OutputForm ref='checkOutputForm' outputData={stepData.output} running={running} />
                    </div>
                </div>
                <div className="steps-action">
                    {stepIndex < steps.length - 1 && <Button type="primary" onClick={() => this.next()}>下一步</Button>}
                    {stepIndex === steps.length - 1 && <Button type="primary" disabled={running} onClick={() => this.storeData()}>保存</Button>}
                    {stepIndex > 0 && <Button style={{ marginLeft: 8 }} onClick={() => this.prev()}>上一步</Button>}
                </div>
            </div>
        );
    };
}

export { NewTaskPage }
