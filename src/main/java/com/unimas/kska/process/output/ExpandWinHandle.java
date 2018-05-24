package com.unimas.kska.process.output;


import com.unimas.kska.process.AppImpl;
import com.unimas.kska.process.KUtils;
import com.unimas.kska.bean.KJson;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * expandWin.enable 数据输出前是否要进行递增窗口统计
 * <p>
 * expandWin.expireTime 超时期限(second)不可为空
 * <p>
 * expandWin.background.threads  后台处理线程数,默认1
 * <p>
 * expandWin.executorPeriod 后台调度周期(mills)默认10s
 * <p>
 * expandWin.countFiled 聚合运算count值的名称,添加在输出的value中.如果output.fields指定输出字段,则要添加到输出字段中
 * <p>
 * expandWin.store.name store name
 */
class ExpandWinHandle implements KUtils {

    private final Logger logger = LoggerFactory.getLogger(ExpandWinHandle.class);

    /**
     * configuration definition
     */
    protected enum CONFIG {
        ENABLE("expandWin.enable"), EXPIRE_TIME("expandWin.expireTime"),
        BACK_THREADS("expandWin.background.threads"), EXECUTOR_PER("expandWin.executorPeriod"),
        COUNT_FIELD("expandWin.countFiled"), STORE_NAME("expandWin.store.name");
        private String value;

        CONFIG(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private ConcurrentMap<String, String> expireMap;
    private ConcurrentMap<String, Integer> counterMap;
    private HTreeMap<String, String> dataMap;
    private ScheduledExecutorService expireExecutor;

    private boolean first = true;
    private final String storeName;
    private final long expireTime;
    private final int backThreads;
    private final long executorPeriod;
    private final String countField;

    private ScheduledExecutorService getDataExecutor;
    private ScheduledFuture getDataFuture;

    ExpandWinHandle(Properties properties) {
        String _storeName = properties.getProperty(CONFIG.STORE_NAME.getValue(), "expandWin");
        this.storeName = _storeName.isEmpty() ? "expandWin" : _storeName;
        this.expireTime = Long.valueOf(nonNullEmpty(properties, CONFIG.EXPIRE_TIME.getValue()));
        String _backThreads = properties.getProperty(CONFIG.BACK_THREADS.getValue(), "1");
        this.backThreads = _backThreads.isEmpty() ? 1 : Integer.parseInt(_backThreads);
        String _executorPer = properties.getProperty(CONFIG.EXECUTOR_PER.getValue(), "10000");
        this.executorPeriod = _executorPer.isEmpty() ? 10000L : Long.valueOf(_executorPer);
        this.countField = nonNullEmpty(properties, CONFIG.COUNT_FIELD.getValue());
        this.expireMap = new ConcurrentHashMap<>();
        this.counterMap = new ConcurrentHashMap<>();
    }

    /**
     * expandWin value
     *
     * @param key        key
     * @param value      value
     * @param biConsumer output kv
     */
    void handle(String key, String value, BiConsumer<String, String> biConsumer) {
        if (first) {
            first = false;
            expireExecutor = Executors.newScheduledThreadPool(backThreads);
            getDataExecutor = Executors.newScheduledThreadPool(1);
            getDataFuture = getDataExecutor.scheduleAtFixedRate(() -> {
                if (!expireMap.isEmpty()) {
                    expireMap.forEach((k, v) -> {
                        try {
                            Map<String, Object> expireV = KJson.readValue(expireMap.remove(k));
                            expireV.put(countField, String.valueOf(counterMap.remove(k)));
                            biConsumer.accept(k, KJson.writeValueAsString(expireV));
                        } catch (IOException e) {
                            logger.error(concat(NEWLINE, "json format error", "value:" + v), e);
                        }
                    });
                }
            }, 2, 1, TimeUnit.SECONDS);
            dataMap = AppImpl.getMapDb().hashMap(storeName)
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .expireAfterUpdate(expireTime, TimeUnit.SECONDS)
                    .expireAfterCreate(expireTime, TimeUnit.SECONDS)
                    .expireOverflow(expireMap)
                    .modificationListener((_key, oldValue, newValue, triggered) -> {
                        if (oldValue == null) { //insert
                            counterMap.put(_key, 1);
                        } else if (newValue != null) { //update
                            int num = counterMap.getOrDefault(_key, 1);
                            counterMap.put(_key, num + 1);
                        } //else {remove action}
                    })
                    .expireExecutor(expireExecutor)
                    .expireExecutorPeriod(executorPeriod)
                    .createOrOpen();
        }
        dataMap.put(key, value);
    }

    void close() {
        logger.debug("#################### expandWinClose ####################");
        if (dataMap != null) dataMap.clearWithExpire();
        if (expireExecutor != null) {
            expireExecutor.shutdown();
            try {
                expireExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {//ignore
            }
            expireExecutor.shutdownNow();
        }
        if (getDataFuture != null) getDataFuture.cancel(true);
        if (getDataExecutor != null) {
            getDataExecutor.shutdown();
            try {
                getDataExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {//ignore
            }
            getDataExecutor.shutdownNow();
        }
        expireMap = null;
        counterMap = null;
        if (dataMap != null) dataMap.close();
    }
}
