package com.unimas.kstream.kafka;

import com.google.gson.reflect.TypeToken;
import com.unimas.kstream.bean.KJson;
import kafka.KsKaClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//import static org.junit.Assert.*;

public class KsKaClientTest {

    private KsKaClient client;

    @Before
    public void setUp() throws Exception {
        client = KsKaClient.apply("10.68.23.11:2181");
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void deleteTopic() {
        client.deleteTopic("test3", false);
    }

    @Test
    public void getAllTopics() throws IOException {
        System.out.println(KJson.writeValue(client.getAllTopics(),
                new TypeToken<List<Map<String, String>>>() {
                }.getType()));
    }

    @Test
    public void getTopicDetail() throws IOException {
        System.out.println(KJson.writeValue(client.getTopicDetail("test1"),
                new TypeToken<List<Map<String, String>>>() {
                }.getType()));
        try {
            Thread.sleep(2 * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(KJson.writeValue(client.getTopicDetail("test1"),
                new TypeToken<List<Map<String, String>>>() {
                }.getType()));
    }

    @Test
    public void getBrokersWithHost() throws IOException {
        System.out.println(KJson.writeValue(client.getBrokersWithHost(), new TypeToken<List<String>>() {
        }.getType()));
    }

    @Test
    public void getBrokers() throws IOException {
        System.out.println(KJson.writeValue(JavaConversions.mapAsJavaMap(client.getBrokers()),
                new TypeToken<Map<Integer, String>>() {
                }.getType()));
    }

    @Test
    public void createTopic() throws Exception {
        client.createTopic("test3", 1, 1, new Properties(), true);
    }

}