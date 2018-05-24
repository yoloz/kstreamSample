package com.unimas.kska.process.output;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

public class OutPutTest {

    Properties properties = new Properties();
    @Before
    public void setUp() throws Exception {
        properties.put(OutPut.CONFIG.OUT_KSOURCE.getValue(),"test");
    }

    @Test
    public void customValue(){
        String value = "{\"filetype\":\"\",\"srcip\":\"10.15.6.134\",\"suspiciousaddr\":\"\",\"dstport\":\"26336\",\"subject\":\"HTTP_Microsoft_Internet_Explorer空字符信息泄露漏洞[MS12-010]\",\"file_direct\":\"\",\"securityid\":\"17\",\"aptid\":\"\",\"type\":\"Alert Log\",\"dt\":\"V0200R0400B20160921\",\"protocol\":\"HTTP\",\"id\":\"152519865\",\"dstip\":\"202.110.253.130\",\"chekresultstatic\":\"\",\"combine_key\":\"HTTP_Microsoft_Internet_Explorer空字符信息泄露漏洞[MS12-010]_10.15.6.134_8080_202.110.253.130_26336\",\"logstash_receive_time\":\"2018-01-16T07:14:27.128Z\",\"level\":30.0,\"count\":1.0,\"log_send_ip\":\"60.208.94.158\",\"security_name\":\"安全漏洞\",\"level_name\":\"中级事件\",\"sender\":\"\",\"name\":\"\",\"devid\":\"\",\"server_name\":\"\",\"log_send_time\":\"Jan 16 15:15:33\",\"dstmac\":\"14-14-4B-6B-75-74\",\"request_resource\":\"\",\"user_name\":\"\",\"suspiciousdomain\":\"\",\"doc_type\":\"ids\",\"result\":\"\",\"ndayflag\":\"\",\"srcmac\":\"14-14-4B-5F-D2-31\",\"value\":\"202.110.253.130\",\"severity\":0.0,\"reserve1\":\"\",\"endtime\":\"1516086929000\",\"reserve3\":\"\",\"evilcodetype\":\"\",\"original_message\":\"\\u003c12\\u003eJan 16 15:15:33 (none) {\\\"dt\\\":\\\"V0200R0400B20160921\\\",\\\"level\\\":30,\\\"id\\\":\\\"152519865\\\",\\\"type\\\":\\\"Alert Log\\\",\\\"time\\\":1516086929,\\\"source\\\":{\\\"ip\\\":\\\"10.15.6.134\\\",\\\"port\\\":8080,\\\"mac\\\":\\\"14-14-4B-5F-D2-31\\\"},\\\"destination\\\":{\\\"ip\\\":\\\"202.110.253.130\\\",\\\"port\\\":26336,\\\"mac\\\":\\\"14-14-4B-6B-75-74\\\"},\\\"count\\\":1,\\\"protocol\\\":\\\"HTTP\\\",\\\"subject\\\":\\\"HTTP_Microsoft_Internet_Explorer空字符信息泄露漏洞[MS12-010]\\\",\\\"message\\\":\\\"nic\\u003d4;Host\\u003d;URL\\u003d;URL长度\\u003d0;Http协议头长度\\u003d111;访问文件\\u003d;Body_Data\\u003d;\\\",\\\"securityid\\\":\\\"17\\\",\\\"attackid\\\":\\\"4003\\\"}\",\"reserve2\":\"\",\"message\":\"nic\\u003d4;Host\\u003d;URL\\u003d;URL长度\\u003d0;Http协议头长度\\u003d111;访问文件\\u003d;Body_Data\\u003d;\",\"email_title\":\"\",\"attack_name\":\"数据盗取-其他\",\"recipient\":\"\",\"srcport\":\"8080\",\"request_domain\":\"\",\"suspiciousurl\":\"\",\"mark\":\"mark\",\"md5\":\"\",\"attackid\":\"4003\"}";
//        properties.put(OutPut.CONFIG.APPEND_NOT_EXIST.getValue(),"true");
//        properties.put(OutPut.CONFIG.OUT_STYLE.getValue(),"custom");
//        properties.put(OutPut.CONFIG.OUT_CUSTOM.getValue(),"{\"srcip\":\"$srcip$\",\"dstip\":\"$dstip$\",\"category\":\"$category$$category1$\",\"count\":\"$win_count$\",\"time\":\"$win_endT$\",\"event\":\"$subject$\"}");
//        SysOut sysOut = new SysOut(properties);
//        try {
//            System.out.println(sysOut.customValue(value));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }
    @After
    public void tearDown() throws Exception {

    }
}