import React, { Component } from 'react';
import { Upload, notification, Button, Icon, Row, Col } from 'antd';
import 'whatwg-fetch';


const props = {
    name: 'file',
    action: 'http://localhost:12583/cii/ks/udFile',
    showUploadList: false,
    onChange(info) {
        // if (info.file.status === 'uploading') {}
        if (info.file.status === 'done') {
            notification.success({
                message: 'success',
                description: `${info.file.name} file uploaded successfully`,
                duration: 1.5
            });
        } else if (info.file.status === 'error') {
            notification.error({
                message: 'fail',
                description: `${info.file.name} file upload failed.`,
                duration: 1.5
            });
        }
    },
};
function download() {
    return fetch('http://localhost:12583/cii/ks/udFile?f=1234', { method: 'get' })
        .then(res => res.blob().then(stream => {
            var params = res.headers.get("Content-Type").split(";");
            var a = document.createElement("a");
            var blob = new Blob([stream], { type: params[0] });
            var url = window.URL.createObjectURL(blob);
            document.body.appendChild(a);
            a.style = "display: none";
            a.href = url;
            a.download = params[1];
            a.click();
            window.URL.revokeObjectURL(url);
        }));
};
class UploadTest extends Component {
    render() {
        return (
            <div>
                <Row style={{ marginTop: '20px' }}>
                    <Col span={8}></Col>
                    <Col span={8} style={{ textAlign: 'center' }}>
                        <Upload {...props}>
                            <Button> <Icon type="upload" />导入</Button>
                        </Upload>
                    </Col>
                    <Col span={8}></Col>
                </Row>
                <Row style={{ marginTop: '20px' }}>
                    <Col span={8}></Col>
                    <Col span={8} style={{ textAlign: 'center' }}>
                        <Button type="primary" onClick={download}>下载</Button>
                    </Col>
                    <Col span={8}></Col>
                </Row>
            </div>
        );
    }
}

export default UploadTest;