package com.ks.process.operation;

import java.text.SimpleDateFormat;

/**
 * produce join test data
 */
public class JoinImplTest {

    private final SimpleDateFormat simpleDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        JoinImplTest joinImplTest = new JoinImplTest();
//        joinImplTest.streamToStream();
        joinImplTest.streamToTable();
    }

    /**
     * join,leftJoin,outerJoin
     */
    private void streamToStream() {
        new Thread(new Producer("stream1", simpleDateFormat) {
            @Override
            public void run() {
                int repeats = 3;
                while (repeats > 0) {
                    insertByTime(20, 2000L,30_000,"");
                    repeats--;
                }
            }
        }, "stream1").start();
        new Thread(new Producer("stream1-1", simpleDateFormat) {
            @Override
            public void run() {
                int repeats = 3;
                while (repeats > 0) {
                    insertByTime(10, 1000L,30_000,"");
                    repeats--;
                }
            }
        }, "stream1-1").start();
    }


    private void tableToTable() {
        new Thread(new Producer("table1", simpleDateFormat) {
            @Override
            public void run() {
                int repeats = 3;
                while (repeats > 0) {
                    insertByTime(10, 1000L,30_000,"");
                    repeats--;
                }
            }
        }, "table1").start();
        new Thread(new Producer("table1-1", simpleDateFormat) {
            @Override
            public void run() {
                int repeats = 3;
                while (repeats > 0) {
                    insertByTime(20, 2000L,30_000,"");
                    repeats--;
                }
            }
        }, "table1-1").start();
    }

    /**
     * table数据读取后,纵使topic数据有效期过了,数据已删除,table中数据仍在
     * 故过期(2min)后,执行删除操作(<key,null>)更新table
     */
    private void streamToTable() {
        new Thread(new Producer("test1", simpleDateFormat) {
            @Override
            public void run() {
                int repeats = 100;
                while (repeats > 0) {
                    insertByTime(1, 2000L,30_000,"");
                    repeats--;
                }
            }
        }, "test1").start();
        new Thread(new Producer("test", simpleDateFormat) {
            @Override
            public void run() {
                int repeats = 1;
                while (repeats > 0) {
                    insertByTime(3, 3000L,30_000,"");
                    repeats--;
                }
            }
        }, "test").start();
        try {
            Thread.sleep(3 * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(new Producer("test", simpleDateFormat) {
            @Override
            public void run() {
                int repeats = 1;
                while (repeats > 0) {
                    insertByTime(3, 3000L,30_000,"");
                    repeats--;
                }
            }
        }, "test").start();
    }
}