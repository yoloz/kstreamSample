package com.unimas.kska.kafka;

import com.google.gson.reflect.TypeToken;
import com.unimas.kska.bean.KJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class KskaClientTest {

    private KskaClient client;

    @Before
    public void setUp() throws Exception {
        client = new KskaClient("10.68.120.111:2181");
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void createTopic() {
        Properties properties = new Properties();
        properties.put("retention.ms", (Integer.parseInt("2") * 24 * 3600 * 1000)+"");
        client.createTopic("createTest", Integer.parseInt("1"), Integer.parseInt("1"), properties);
    }

    @Test
    public void getAllTopic() {
        try {
            System.out.println(KJson.writeValue(client.getAllTopics(),
                    new TypeToken<List<Map<String, String>>>() {
                    }.getType()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void describeTopic() {
        try {
            System.out.println(KJson.writeValue(client.getTopicDetail("createTest"),
                    new TypeToken<List<Map<String, String>>>() {
                    }.getType()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void deleteTopic() {
        client.deleteTopic("createTest", false);
    }
}