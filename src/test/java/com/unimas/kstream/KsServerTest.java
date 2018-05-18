package com.unimas.kstream;

import com.unimas.kstream.bean.KJson;
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

    private void outPutResult(CloseableHttpResponse response) throws IOException {
        StatusLine statusLine = response.getStatusLine();
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) System.out.println(EntityUtils.toString(resEntity));
        } else System.out.println(statusLine);
    }


}