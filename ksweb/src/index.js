import React from 'react';
import ReactDOM from 'react-dom';
import { getVersion } from './services/restApi';
import './index.css';
import logo from './logo.svg';
import { Layout, Menu, Breadcrumb, Icon, Button, } from 'antd';
import { GeneralTable } from './container/generalInfoContainer'
import { CfgMgrTable } from './container/cfMgrContainer'
import { NewTaskPage } from './container/newTaskContainer'
// import registerServiceWorker from './registerServiceWorker';

const { Header, Content, Footer, Sider } = Layout;

class IndexClass extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            runners: [], //运行的任务,不可修改，不可删除
            running: false, //临时缓存跳转newTask
            ksUrl: '', //从配置文件中获取
            version: '', //版本
            currentMenu: 'system', //sidebar切换参数
            stepIndex: 0, //任务step的index
            stepData: { main: {}, sources: [], operations: [], output: {} },//任务step对应的index的数据
            collapsed: false, //sidebar是否缩小
        };
        this.initVersion();
    }
    backToCfgMgr = () => {
        this.setState({
            currentMenu: 'confmgr',
            running: false,
            stepIndex: 0,
            stepData: { main: {}, sources: [], operations: [], output: {} },
        });
    };
    toNewTaskPage = (step, record) => {
        const runners = this.state.runners;
        const data = (record === undefined ? { main: {}, sources: [], operations: [], output: {} } : record);
        // console.log(data);
        this.setState({
            currentMenu: 'newTask',
            running: runners.some((item) => { return item === data.main.application_id }),
            stepIndex: step === undefined ? 0 : step,
            stepData: data,
        });
        if (window.generalInfoInterval !== undefined && window.generalInfoInterval !== 'undefined') {
            window.clearInterval(window.generalInfoInterval);
        }
    };
    updateRunners = (arr) => {
        this.setState({
            runners: arr,
        });
    };
    initVersion() {
        getVersion().then((data) => {
            // console.log(JSON.stringify(data));
            if (data.success) {
                this.setState({
                    version: data.version
                });
            } else {
                this.setState({
                    version: ''
                });
            }
        });
    };
    onCollapse = (collapsed) => {
        this.setState({ collapsed });
    };
    changeMenu = (e) => {
        this.setState({
            currentMenu: e.key
        });
        if (e.key === 'system') {
            // window.clearInterval(window.cfMgrInfoInterval);
            // window.clearInterval(window.machineInterval);
        } else if (e.key === 'confmgr') {
            window.clearInterval(window.generalInfoInterval);
            // window.clearInterval(window.machineInterval);
        }

    };
    render() {
        return (
            <Layout style={{ minHeight: '100vh' }}>
                <Sider collapsible collapsed={this.state.collapsed} onCollapse={this.onCollapse}>
                    <div className="menuLogo" >{this.state.collapsed === false ?
                        (<img src={logo} alt="react" style={{ marginLeft: 60 + 'px' }} />) : (<img src={logo} alt="react"></img>)}</div>
                    <Menu theme="dark" defaultSelectedKeys={['system']} mode="inline" onClick={this.changeMenu}>
                        <Menu.Item key="system"><Icon type="home" /><span>首页</span></Menu.Item>
                        <Menu.Item key="confmgr"><Icon type="tool" /><span>配置管理</span></Menu.Item>
                    </Menu>
                </Sider>
                <Layout>
                    <Header className="header" style={{ background: '#fff', padding: 0 }}>KStream助手{this.state.version}</Header>
                    {this.state.currentMenu === 'system' ?
                        (<Content style={{ margin: '0 16px' }}>
                            <Breadcrumb style={{ margin: '16px 0' }}><Breadcrumb.Item>首页</Breadcrumb.Item></Breadcrumb>
                            <div style={{ padding: 24, background: '#fff', minHeight: 360 }}>
                                <GeneralTable updateRunners={this.updateRunners.bind(this)} />
                            </div>
                        </Content>) : null}
                    {this.state.currentMenu === 'confmgr' ?
                        (<Content style={{ margin: '0 16px' }}>
                            <Breadcrumb style={{ margin: '16px 0' }}><Breadcrumb.Item>配置管理</Breadcrumb.Item></Breadcrumb>
                            <div style={{ padding: 20, background: '#fff', minHeight: 360 }}>
                                <div style={{ marginBottom: '10px' }}>
                                    <Button type="primary" icon='plus' ghost onClick={() => this.toNewTaskPage()}>新建任务</Button>
                                </div>
                                <CfgMgrTable toNewTaskPage={this.toNewTaskPage.bind(this)} />
                            </div>
                        </Content>) : null}
                    {this.state.currentMenu === 'newTask' ?
                        (<Content style={{ margin: '0 16px' }}>
                            <Breadcrumb style={{ margin: '16px 0' }}><Breadcrumb.Item>新建任务</Breadcrumb.Item></Breadcrumb>
                            <div style={{ padding: 24, background: '#fff', minHeight: 360 }}>
                                <div style={{ marginBottom: '15px' }}>
                                    <Button type="primary" icon='rollback' ghost onClick={this.backToCfgMgr}> 返回</Button>
                                </div>
                                <NewTaskPage defaultStep={this.state.stepIndex} defaultData={this.state.stepData}
                                    running={this.state.running} backToCfgMgr={this.backToCfgMgr.bind(this)} />
                            </div>
                        </Content>) : null}
                    <Footer style={{ textAlign: 'center' }}>使用愉快!</Footer>
                </Layout>
            </Layout>
        );
    }
};

ReactDOM.render(<IndexClass />, document.getElementById('root'));
// registerServiceWorker();