package com.unimas.kska;

import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import com.unimas.kska.bean.KJson;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

//import static org.junit.Assert.*;

public class KsServerTest {

    private CloseableHttpClient httpClient;

    @Before
    public void setUp() {
        httpClient = HttpClientBuilder.create().build();
    }

    @After
    public void tearDown() throws Exception {
        httpClient.close();
    }

    @Test
    public void storeMain() throws IOException {
        Map<String, Object> map = new HashMap<>(2);
        map.put("type", "main");
        map.put("id", "5aefbd38284708");
        Map<String, String> value = new HashMap<>(3);
        value.put("app_name", "test4");
        value.put("app_desc", "test4");
        value.put("ds_id", "5af0118126ac56");
        map.put("value", value);
        String param = KJson.writeValueAsString(map);
        HttpPost httpPost = new HttpPost("http://10.68.13.120:12583/cii/ks/storeApp");
        httpPost.setEntity(new StringEntity(param, Charset.forName("utf-8")));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            outPutResult(response);
        }
    }

    @Test
    public void deployApp() throws IOException {
        Map<String, Object> map = new HashMap<>(2);
        map.put("service_id", "438f4836328c4ba8aca9a672f8aada7c");
        String param = KJson.writeValueAsString(map);
        HttpPost httpPost = new HttpPost("http://10.68.13.120:12583/cii/ks/deployApp");
        httpPost.setEntity(new StringEntity(param, Charset.forName("utf-8")));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            outPutResult(response);
        }
    }

    @Test
    public void deleteApp() throws IOException {
        Map<String, Object> map = new HashMap<>(2);
        map.put("type", "main");
        map.put("service_id", "438f4836328c4ba8aca9a672f8aada7c");
        String param = KJson.writeValueAsString(map);
        HttpPost httpPost = new HttpPost("http://10.68.13.120:12583/cii/ks/deleteApp");
        httpPost.setEntity(new StringEntity(param, Charset.forName("utf-8")));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            outPutResult(response);
        }
    }

    @Test
    public void storeInput() throws IOException {
        String param = "{\"type\":\"input\",\"value\":{\"ks_type\":\"stream\",\"ks_topics\":\"test\",\"ks_time_name\":null,\"ks_time_type\":\"long\",\"ks_time_format\":null,\"ks_time_lang\":null,\"ks_time_offsetId\":null},\"service_id\":\"9411c7a3505941fb9dce1ec9935731f0\"}";
        HttpPost httpPost = new HttpPost("http://10.68.13.120:12583/cii/ks/storeApp");
        httpPost.setEntity(new StringEntity(param, Charset.forName("utf-8")));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            outPutResult(response);
        }
    }

    @Test
    public void storeOutput() throws IOException {
        String param = "{\"service_id\":\"5804d9f40372438980ebc3ff0047e171\",\"type\":\"output\",\"values\":{\"output_ks_name\":\"qwerrr\",\"output_fields\":null,\"output_targets\":[\"sysout\"],\"output_target_kafka_address\":null,\"output_target_kafka_topic\":null,\"output_target_zbus_address\":null,\"output_target_zbus_mq\":null,\"expandWin_enable\":\"false\",\"expandWin_expireTime\":null,\"expandWin_background_threads\":1,\"expandWin_executorPeriod\":10000,\"expandWin_countFiled\":null,\"format_enable\":\"false\",\"format_pattern\":null}}";
        HttpPost httpPost = new HttpPost("http://10.68.13.120:12583/cii/ks/storeApp");
        httpPost.setEntity(new StringEntity(param, Charset.forName("utf-8")));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            outPutResult(response);
        }
    }

    @Test
    public void getAppConf() throws IOException {
        String param = "{\"id\":\"5af007ccd676d5\",\"type\":\"operation\"}";
        HttpPost httpPost = new HttpPost("http://10.68.13.120:12583/cii/ks/getAppConf");
        httpPost.setEntity(new StringEntity(param, Charset.forName("utf-8")));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            outPutResult(response);
        }
    }

    @Test
    public void getLocalIp() throws IOException {
        HttpGet httpGet = new HttpGet("http://10.68.13.120:12583/cii/ka/getLocalIp");
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            outPutResult(response);
        }
    }

    @Test
    public void parseValue() throws IOException {
        String value = "{\"filetype\":\"\",\"srcip\":\"10.15.6.73\",\"suspiciousaddr\":\"\",\"dstport\":\"53\",\"subject\":\"DNS_内网向外请求\",\"file_direct\":\"\",\"securityid\":\"12\",\"aptid\":\"\",\"type\":\"Alert Log\",\"dt\":\"V0200R0400B20160921\",\"protocol\":\"DNS\",\"id\":\"-1995126411\",\"dstip\":\"216.239.36.10\",\"chekresultstatic\":\"\",\"combine_key\":\"DNS_内网向外请求_10.15.6.73_62145_216.239.36.10_53\",\"logstash_receive_time\":\"2018-01-12T08:05:48.473Z\",\"level\":20,\"count\":1,\"log_send_ip\":\"60.208.94.158\",\"security_name\":\"可疑行为\",\"level_name\":\"低级事件\",\"sender\":\"\",\"name\":\"\",\"devid\":\"\",\"server_name\":\"\",\"log_send_time\":\"Jan 12 16:06:49\",\"dstmac\":\"C4-47-3F-49-AE-2B\",\"request_resource\":\"\",\"user_name\":\"\",\"suspiciousdomain\":\"\",\"doc_type\":\"ids\",\"result\":\"\",\"ndayflag\":\"\",\"srcmac\":\"00-50-56-8F-2D-70\",\"severity\":0,\"reserve1\":\"\",\"endtime\":\"1515744409000\",\"reserve3\":\"\",\"evilcodetype\":\"\",\"original_message\":\"<12>Jan 12 16:06:49 (none) {\\\"dt\\\":\\\"V0200R0400B20160921\\\",\\\"level\\\":20,\\\"id\\\":\\\"-1995126411\\\",\\\"type\\\":\\\"Alert Log\\\",\\\"time\\\":1515744409,\\\"source\\\":{\\\"ip\\\":\\\"10.15.6.73\\\",\\\"port\\\":62145,\\\"mac\\\":\\\"00-50-56-8F-2D-70\\\"},\\\"destination\\\":{\\\"ip\\\":\\\"216.239.36.10\\\",\\\"port\\\":53,\\\"mac\\\":\\\"C4-47-3F-49-AE-2B\\\"},\\\"count\\\":1,\\\"protocol\\\":\\\"DNS\\\",\\\"subject\\\":\\\"DNS_内网向外请求\\\",\\\"message\\\":\\\"nic=4;源IP地址=10.15.6.73;DNS查询类型=1;请求域名=clients5.google.com;数据=c4 01 00 00 00 01 00 00 00 00 00 00 08 63 6c 69 65 6e 74 73 35 06 67 6f 6f 67 6c 65 03 63 6f 6d 00 00 01 00 01;\\\",\\\"securityid\\\":\\\"12\\\",\\\"attackid\\\":\\\"9000\\\"}\",\"reserve2\":\"\",\"message\":\"nic=4;源IP地址=10.15.6.73;DNS查询类型=1;请求域名=clients5.google.com;数据=c4 01 00 00 00 01 00 00 00 00 00 00 08 63 6c 69 65 6e 74 73 35 06 67 6f 6f 67 6c 65 03 63 6f 6d 00 00 01 00 01;\",\"email_title\":\"\",\"attack_name\":\"其他类攻击事件-其他\",\"recipient\":\"\",\"srcport\":\"62145\",\"request_domain\":\"\",\"suspiciousurl\":\"\",\"mark\":\"mark\",\"md5\":\"\",\"attackid\":\"9000\"}";
        Map<String, String> map = ImmutableMap.of("src", value, "logtype", "json",
                "separator", "", "head", "false","keyword","");
        String param = KJson.writeValue(map, new TypeToken<Map<String, String>>() {
        }.getType());
        HttpPost httpPost = new HttpPost("http://10.68.120.12:7914/parse");
        httpPost.setEntity(new StringEntity(param, Charset.forName("utf-8")));
        httpPost.addHeader("Content-Type","application/json");
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            outPutResult(response);
        }
    }


    private void outPutResult(CloseableHttpResponse response) throws IOException {
        StatusLine statusLine = response.getStatusLine();
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) System.out.println(EntityUtils.toString(resEntity));
        } else System.out.println(statusLine);
    }


}