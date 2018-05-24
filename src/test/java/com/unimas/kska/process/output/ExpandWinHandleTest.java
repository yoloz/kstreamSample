package com.unimas.kska.process.output;


import com.unimas.kska.process.operation.Producer;

public class ExpandWinHandleTest {

    public static void main(String[] args) {
//        HTreeMap<String, String> dataMap = DBMaker
//                .memoryShardedHashMap(8)
//                .keySerializer(Serializer.STRING)
//                .valueSerializer(Serializer.STRING)
//                .createOrOpen();
//        System.out.println("segment^:" + dataMap.getConcShift() + "nodeSize^:"
//                + dataMap.getDirShift() + "level:" + dataMap.getLevels());
//        System.out.println("哈兮表大小：segment*nodeSize^level");
//        dataMap.close();

        new Thread(new Producer("test") {
            @Override
            public void run() {
                String value = "{\"category\":\"scanner\",\"value\":\"190.48.94.20\"}";
                try {
                    insertByCounter(0, 10, value);
                    System.out.println("sleep 70000====================");
                    Thread.sleep(70000);
                    System.out.println("sleep 70000 finish====================");
                    insertByCounter(0, 6, value);
                    System.out.println("sleep 20000====================");
                    Thread.sleep(20000);
                    System.out.println("sleep 20000 finish====================");
                    insertByCounter(0, 5, value);
                    System.out.println("sleep 20000====================");
                    Thread.sleep(20000);
                    System.out.println("sleep 20000 finish====================");
                    insertByCounter(0, 4, value);
                    System.out.println("sleep 20000====================");
                    Thread.sleep(20000);
                    System.out.println("sleep 20000 finish====================");
                    insertByCounter(0, 3, value);
                    System.out.println("sleep 10000====================");
                    Thread.sleep(10000);
                    System.out.println("sleep 10000 finish====================");
                    System.out.println("sleep 10000====================");
                    Thread.sleep(10000);
                    System.out.println("sleep 10000 finish====================");
                    System.out.println("sleep 10000====================");
                    Thread.sleep(10000);
                    System.out.println("sleep 10000 finish====================");
                    System.out.println("sleep 10000====================");
                    Thread.sleep(10000);
                    System.out.println("sleep 10000 finish====================");
                    System.out.println("sleep 10000====================");
                    Thread.sleep(10000);
                    System.out.println("sleep 10000 finish====================");
                    System.out.println("sleep 10000====================");
                    Thread.sleep(10000);
                    System.out.println("sleep 10000 finish====================");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

//        Properties properties = new Properties();
//        properties.put(ExpandWinHandle.CONFIG.EXPIRE_TIME.getValue(), "60");
//        properties.put(ExpandWinHandle.CONFIG.EXECUTOR_PER.getValue(), "1000");
//        properties.put(ExpandWinHandle.CONFIG.COUNT_FIELD.getValue(), "count");
//        ExpandWinHandle expandWinHandle = new ExpandWinHandle(properties);
//        Runtime.getRuntime().addShutdownHook(new Thread(expandWinHandle::close));
//        try {
//            String value = "{\"category\":\"scanner\",\"value\":\"190.48.94.20\"}";
//            int key = 0;
//            while (key < 10) {
//                expandWinHandle.handle(String.valueOf(key), value, (k, v) -> System.out.println(k + "==>" + v));
//                ++key;
//            }
//            System.out.println("sleep 70000====================");
//            Thread.sleep(70000);
//            System.out.println("sleep 70000 finish====================");
//            key = 0;
//            while (key < 6) {
//                expandWinHandle.handle(String.valueOf(key), value, (k, v) -> System.out.println(k + "==>" + v));
//                ++key;
//            }
//            System.out.println("sleep 20000====================");
//            Thread.sleep(20000);
//            System.out.println("sleep 20000 finish====================");
//            key = 0;
//            while (key < 5) {
//                expandWinHandle.handle(String.valueOf(key), value, (k, v) -> System.out.println(k + "==>" + v));
//                ++key;
//            }
//            System.out.println("sleep 20000====================");
//            Thread.sleep(20000);
//            System.out.println("sleep 20000 finish====================");
//            key = 0;
//            while (key < 4) {
//                expandWinHandle.handle(String.valueOf(key), value, (k, v) -> System.out.println(k + "==>" + v));
//                ++key;
//            }
//            System.out.println("sleep 20000====================");
//            Thread.sleep(20000);
//            System.out.println("sleep 20000 finish====================");
//            key = 0;
//            while (key < 3) {
//                expandWinHandle.handle(String.valueOf(key), value, (k, v) -> System.out.println(k + "==>" + v));
//                ++key;
//            }
//            System.out.println("sleep 10000====================");
//            Thread.sleep(10000);
//            System.out.println("sleep 10000 finish====================");
//            System.out.println("sleep 10000====================");
//            Thread.sleep(10000);
//            System.out.println("sleep 10000 finish====================");
//            System.out.println("sleep 10000====================");
//            Thread.sleep(10000);
//            System.out.println("sleep 10000 finish====================");
//            System.out.println("sleep 10000====================");
//            Thread.sleep(10000);
//            System.out.println("sleep 10000 finish====================");
//            System.out.println("sleep 10000====================");
//            Thread.sleep(10000);
//            System.out.println("sleep 10000 finish====================");
//            System.out.println("sleep 10000====================");
//            Thread.sleep(10000);
//            System.out.println("sleep 10000 finish====================");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

}
