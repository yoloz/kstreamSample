package com.unimas.kska.process.operation.shandongTest;

import com.unimas.kska.process.operation.Producer;

import java.time.OffsetDateTime;

public class IocsDataTest {

    public static void main(String[] args) {
        System.out.println(OffsetDateTime.now().toString());
        new Thread(new Producer("iocs") {
            @Override
            public void run() {
                String value = "{\"category\":\"scanner\",\"value\":\"190.48.94.20\"}";
                insertByCounter(1, 10, value);
                System.out.println(OffsetDateTime.now().toString());
            }
        }).start();
    }
}
