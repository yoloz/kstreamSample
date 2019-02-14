package com.ks.bean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class KJsonTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void readValue() {
        String str = "{\"main\":{\"application_id\":\"iRNVmkstUKdVxCOD\",\"application_name\":\"11\",\"bootstrap_servers\":\"11\",\"ks_zookeeper_url\":null,\"cache_max_bytes_buffering\":10485760,\"num_stream_threads\":1,\"buffered_records_per_partition\":1000,\"commit_interval_ms\":30000,\"auto_offset_reset\":\"latest\"},\"sources\":[{\"ks_name\":\"11\",\"ks_type\":\"stream\",\"ks_topics\":\"11\",\"ks_time_name\":null,\"ks_time_type\":\"long\",\"ks_time_format\":null,\"ks_time_lang\":null,\"ks_time_offsetId\":null}],\"operations\":[{\"operation_name\":\"11\",\"operation_ks_name\":\"11\",\"join_ks_name\":null,\"operation_operator\":\"convertKV\",\"kv_fields_noExist_append\":\"false\",\"kv_key_fields\":null,\"kv_key_fields_type\":\"value\",\"kv_value_fields\":null}],\"output\":{\"output_ks_name\":\"11\",\"output_fields\":null,\"output_targets\":\"sysout\",\"output_target_kafka_address\":null,\"output_target_kafka_topic\":null,\"output_target_zbus_address\":null,\"output_target_zbus_mq\":null,\"expandWin_enable\":\"false\",\"expandWin_expireTime\":null,\"expandWin_background_threads\":1,\"expandWin_executorPeriod\":10000,\"expandWin_countFiled\":null,\"format_enable\":\"false\",\"format_pattern\":null}}";
        try {
            Map<String, Object> map = KJson.readValue(str);
            System.out.println(map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}