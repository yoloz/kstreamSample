package com.unimas.kska.process.operation;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 *
 */
public abstract class Producer implements Runnable {
    protected final KafkaProducer<String, String> producer;
    protected final String topic;
    private SimpleDateFormat simpleDateFormat;

    private Producer(String topic, String keySer, String valueSer, SimpleDateFormat simpleDateFormat) {
        Properties props = new Properties();
        props.put("security.protocol", "PLAINTEXT");
        props.put("bootstrap.servers", "10.68.23.11:9092");
        props.put("client.id", "DemoProducer");
        props.put("key.serializer", keySer);
        props.put("value.serializer", valueSer);
        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
        this.simpleDateFormat = simpleDateFormat;
    }

     protected Producer(String topic) {
        this(topic, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    Producer(String topic, SimpleDateFormat simpleDateFormat) {
        this(topic, StringSerializer.class.getName(), StringSerializer.class.getName(), simpleDateFormat);
    }


    /**
     * <p>
     * key:counter
     * value:name[threadName_'key']time{@link SimpleDateFormat}
     *
     * @param startCounter   起始key
     * @param sleepPerRecord sleep per record
     * @param totalTime      total time for insert
     * @param value          if value is empty,value will be replaced
     */
     void insertByTime(final int startCounter, final long sleepPerRecord, final int totalTime,
                      String value) {
        int key = startCounter;
        long timeDiff = 0;
        long startTime = System.currentTimeMillis();
        final String threadName = Thread.currentThread().getName();
        while (timeDiff < totalTime) {
            long now = System.currentTimeMillis();
            if (value.isEmpty()) {
                value = "{" + "\"name\":\"" + threadName + "_" + key + "\"," +
                        "\"time\":\"" + simpleDateFormat.format(new Date(now)) + "\"}";
            }
            try {
                producer.send(new ProducerRecord<>(topic, String.valueOf(key), value)).get();
                System.out.printf("%s send message %s\n", threadName, value);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            timeDiff = now - startTime;
            ++key;
            try {
                Thread.sleep(sleepPerRecord);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * <p>
     * key:counter
     * value:name[threadName_'key']time{@link SimpleDateFormat}
     *
     * @param startCounter 起始key
     * @param totalCounter total counter for insert
     * @param value        if value is empty,value will be replaced
     */
    protected void insertByCounter(final int startCounter, final int totalCounter, String value) {
        int key = startCounter;
        final String threadName = Thread.currentThread().getName();
        while (key < totalCounter) {
            long now = System.currentTimeMillis();
            if (value.isEmpty()) {
                value = "{" + "\"name\":\"" + threadName + "_" + key + "\"," +
                        "\"time\":\"" + simpleDateFormat.format(new Date(now)) + "\"}";
            }
            try {
                producer.send(new ProducerRecord<>(topic, String.valueOf(key), value)).get();
//                System.out.printf("%s send message %s\n", threadName, value);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            ++key;
        }
    }
}
