package com.unimas.kstream.process.operation;


import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * window窗口过后再来数据,还是会进入原先窗口中并更新count值
 */
public class WindowTest {


    public static void main(String[] args) {
//        new Thread(new Producer("test1", new SimpleDateFormat("yyyy-MM-dd HH:mm")) {
//            @Override
//            public void run() {
//                insertByTime(1, 2000L);
//            }
//        }).start();
        new Thread(new Producer("test1", new SimpleDateFormat("yyyy-MM-dd HH:mm")) {
            @Override
            public void run() {
                Map<Integer, String> listMsg = new HashMap<>();
                DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                        .appendPattern("uuuu-MM-dd HH:mm:ss").toFormatter();
                int key = 1;
                long timeDiff = 0;
                long startTime = System.currentTimeMillis();
                while (timeDiff < 30_000) {
                    LocalDateTime localDateTime = LocalDateTime.now();
                    listMsg.put(key, packageMsg(key, dateTimeFormatter.format(localDateTime.minusSeconds(1L))));
                    ++key;
                    listMsg.put(key, packageMsg(key, dateTimeFormatter.format(localDateTime)));
                    ++key;
                    listMsg.put(key, packageMsg(key, dateTimeFormatter.format(localDateTime.plusSeconds(1L))));
                    timeDiff = System.currentTimeMillis() - startTime;
                    ++key;
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for (int i = 2; i > 0; i--) {
                    listMsg.forEach(this::sendMsg);
                }
            }

            private String packageMsg(int key, String time) {
                return "{" + "\"name\":\"" + "msg_" + key + "\"," + "\"time\":\"" + time + "\"}";
            }

            private void sendMsg(int key, String msg) {
                try {
                    producer.send(new ProducerRecord<>(topic, String.valueOf(key), msg)).get();
                    System.out.printf("send message %s\n", msg);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @Test
    public void calStartTime() {
        long timestamp = 125;
        long sizeMs = 5;
        long advanceMs = 5;
        System.out.println(Math.max(0L, timestamp - sizeMs + advanceMs));
        System.out.println(Math.max(0L, timestamp - sizeMs + advanceMs) / advanceMs * advanceMs);
    }

    @Test
    public void TimeWindowsForTest() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        OffsetDateTime now = OffsetDateTime.now();
        long l = now.toInstant().toEpochMilli();
        System.out.println(formatter.format(now) + "==>" + l);
        TimeWindows.of(124L).windowsFor(l).forEach((k, v) -> {
            System.out.println(String.format("start:%o,window:%s", k, v));
        });
    }
}
