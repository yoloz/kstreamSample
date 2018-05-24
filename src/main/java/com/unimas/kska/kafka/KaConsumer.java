package com.unimas.kska.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Collections;
import java.util.Properties;
import java.util.Random;

public class KaConsumer implements AutoCloseable {

    private final KafkaConsumer<String, String> consumer;
    private final String topic;

    public KaConsumer(String url, String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, new Random().nextInt(100000));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new KafkaConsumer<>(props);
        this.topic = topic;
    }

    public String getOneValue() {
        String result = "";
        consumer.subscribe(Collections.singletonList(this.topic));
//        ConsumerRecords<String, String> records = consumer.poll(1000);
        Iterable<ConsumerRecord<String, String>> records = consumer.poll(1000).records(topic);
        if (records.iterator().hasNext()) {
            result = records.iterator().next().value();
        }
        return result;
    }

    @Override
    public void close() {
        if (consumer != null) consumer.close();
    }
}
