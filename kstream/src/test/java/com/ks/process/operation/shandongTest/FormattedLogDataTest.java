package com.ks.process.operation.shandongTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ks.process.operation.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class FormattedLogDataTest {

    public static void main(String[] args) {
        new Thread(new Producer("formattedlog") {
            String valueFormat = "{\"filetype\":\"\",\"srcip\":\"10.15.6.73\",\"suspiciousaddr\":\"\",\"dstport\":\"53\",\"subject\":\"DNS_内网向外请求\",\"file_direct\":\"\",\"securityid\":\"12\",\"aptid\":\"\",\"type\":\"Alert Log\",\"dt\":\"V0200R0400B20160921\",\"protocol\":\"DNS\",\"id\":\"-1995126411\",\"dstip\":\"216.239.36.10\",\"chekresultstatic\":\"\",\"combine_key\":\"DNS_内网向外请求_10.15.6.73_62145_216.239.36.10_53\",\"logstash_receive_time\":\"2018-01-12T08:05:48.473Z\",\"level\":20,\"count\":1,\"log_send_ip\":\"60.208.94.158\",\"security_name\":\"可疑行为\",\"level_name\":\"低级事件\",\"sender\":\"\",\"name\":\"\",\"devid\":\"\",\"server_name\":\"\",\"log_send_time\":\"Jan 12 16:06:49\",\"dstmac\":\"C4-47-3F-49-AE-2B\",\"request_resource\":\"\",\"user_name\":\"\",\"suspiciousdomain\":\"\",\"doc_type\":\"ids\",\"result\":\"\",\"ndayflag\":\"\",\"srcmac\":\"00-50-56-8F-2D-70\",\"severity\":0,\"reserve1\":\"\",\"endtime\":\"1515744409000\",\"reserve3\":\"\",\"evilcodetype\":\"\",\"original_message\":\"<12>Jan 12 16:06:49 (none) {\\\"dt\\\":\\\"V0200R0400B20160921\\\",\\\"level\\\":20,\\\"id\\\":\\\"-1995126411\\\",\\\"type\\\":\\\"Alert Log\\\",\\\"time\\\":1515744409,\\\"source\\\":{\\\"ip\\\":\\\"10.15.6.73\\\",\\\"port\\\":62145,\\\"mac\\\":\\\"00-50-56-8F-2D-70\\\"},\\\"destination\\\":{\\\"ip\\\":\\\"216.239.36.10\\\",\\\"port\\\":53,\\\"mac\\\":\\\"C4-47-3F-49-AE-2B\\\"},\\\"count\\\":1,\\\"protocol\\\":\\\"DNS\\\",\\\"subject\\\":\\\"DNS_内网向外请求\\\",\\\"message\\\":\\\"nic=4;源IP地址=10.15.6.73;DNS查询类型=1;请求域名=clients5.google.com;数据=c4 01 00 00 00 01 00 00 00 00 00 00 08 63 6c 69 65 6e 74 73 35 06 67 6f 6f 67 6c 65 03 63 6f 6d 00 00 01 00 01;\\\",\\\"securityid\\\":\\\"12\\\",\\\"attackid\\\":\\\"9000\\\"}\",\"reserve2\":\"\",\"message\":\"nic=4;源IP地址=10.15.6.73;DNS查询类型=1;请求域名=clients5.google.com;数据=c4 01 00 00 00 01 00 00 00 00 00 00 08 63 6c 69 65 6e 74 73 35 06 67 6f 6f 67 6c 65 03 63 6f 6d 00 00 01 00 01;\",\"email_title\":\"\",\"attack_name\":\"其他类攻击事件-其他\",\"recipient\":\"\",\"srcport\":\"62145\",\"request_domain\":\"\",\"suspiciousurl\":\"\",\"mark\":\"mark\",\"md5\":\"\",\"attackid\":\"9000\"}";
            DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd'T'HH:mm:ss.SSSX").toFormatter();
            ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public void run() {
                try {
                    Map formatValue = objectMapper.readValue(valueFormat, HashMap.class);
                    for (int i = 0; i < 50; i++) {
                        send_msg(formatValue);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @SuppressWarnings("unchecked")
            void send_msg(Map formatValue) throws JsonProcessingException {
                int key = 0;
                while (key < 10) {
                    OffsetDateTime now = OffsetDateTime.now();
                    formatValue.put("srcip", String.valueOf(key));
                    formatValue.put("dstip", String.valueOf(new Random().nextInt(4_000_000)));
                    formatValue.put("logstash_receive_time", dateTimeFormatter.format(now));
                    String value = objectMapper.writeValueAsString(formatValue);
                    try {
                        producer.send(new ProducerRecord<>(topic, null, value)).get();
//                        System.out.printf("send message %s\n", value);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    ++key;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
