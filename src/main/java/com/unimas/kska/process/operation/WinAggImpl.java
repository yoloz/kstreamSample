package com.unimas.kska.process.operation;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.unimas.kska.bean.KJson;
import com.unimas.kska.error.KConfigException;
import com.unimas.kska.error.KRunException;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * window.uncover.fields 不覆盖的field,(int自动求和,string尾部追加).如:f1,f2,f3...
 * <p>
 * window.uncover.field.repeat 不覆盖的field追加是否允许重复,对于int数据无效.默认false.如:true/false
 * <p>
 * window.uncover.field.interval 不覆盖的field间隔符.默认','
 * <p>
 * window.uncover.field.prefix uncover内部字段前缀与uncover field区别.默认"__"
 * <p>
 * window.sizeMs window size(milli).默认600_000
 * <p>
 * window.advanceMs default window size
 * <p>
 * window.retentionMs default window size
 * <p>
 * window.count 聚合运算count值的名称,添加在输出的value中.注意：如果配置输出字段,则添加到输出字段
 * <p>
 * window.startTime 窗口起始时间field名称,未配置则不输出.注意：如果配置输出字段,则添加到输出字段
 * <p>
 * window.endTime 窗口结束时间field名称,未配置则不输出.注意：如果配置输出字段,则添加到输出字段
 * <p>
 * window.store.name queryable store name.默认"aggregation",一次执行中有多个window操作,则store.name不能相同
 */
class WinAggImpl extends Operation {

    private final Logger logger = LoggerFactory.getLogger(WinAggImpl.class);

    /**
     * configuration definition
     */
    private enum CONFIG {
        UNCOVER_FIELDS("window.uncover.fields"), UNCOVER_REPEAT("window.uncover.field.repeat"),
        UNCOVER_INTERVAL("window.uncover.field.interval"), UNCOVER_PRE("window.uncover.field.prefix"),
        WIN_SIZE("window.sizeMs"), WIN_ADVANCE("window.advanceMs"),
        WIN_RETENTION("window.retentionMs"), WIN_COUNT("window.count"),
        WIN_START("window.startTime"), WIN_END("window.endTime"), WIN_STORE("window.store.name");
        private String value;

        CONFIG(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    private final long windowMs;
    private final long advanceMs;
    private long retentionMs;
    private final String countField;
    private final String winStartField;
    private final String winEndField;
    private ImmutableList<String> unCoverKeys;
    private final boolean uncoverRepeat;
    private final String uncoverInterval;
    private final String unCoverPre;
    private final String storeName;
    private final boolean addWinStart;
    private final boolean addWinEnd;


    WinAggImpl(Properties properties) {
        super(properties);
        String _winSize = properties.getProperty(CONFIG.WIN_SIZE.getValue(), "600000");
        this.windowMs = _winSize.isEmpty() ? 600_000L : Long.valueOf(_winSize);
        String _advanceMs = properties.getProperty(CONFIG.WIN_ADVANCE.getValue(), _winSize);
        this.advanceMs = _advanceMs.isEmpty() ? windowMs : Long.valueOf(_advanceMs);
        String _retentionMs = properties.getProperty(CONFIG.WIN_RETENTION.getValue(), _winSize);
        this.retentionMs = _retentionMs.isEmpty() ? windowMs : Long.valueOf(_retentionMs);
        if (retentionMs < windowMs) {
            logger.warn(concat(" ", CONFIG.WIN_RETENTION.getValue(), "is lower than", CONFIG.WIN_SIZE.getValue()));
            retentionMs = windowMs;
        }
        this.countField = nonNullEmpty(properties, CONFIG.WIN_COUNT.getValue());
        this.unCoverKeys = split(properties.getProperty(CONFIG.UNCOVER_FIELDS.getValue()), COMMA);
        String _uncoverRepeat = properties.getProperty(CONFIG.UNCOVER_REPEAT.getValue(), "false");
        this.uncoverRepeat = _uncoverRepeat.isEmpty() ? false : Boolean.valueOf(_uncoverRepeat);
        String _uncoverInterval = properties.getProperty(CONFIG.UNCOVER_INTERVAL.getValue(), COMMA);
        this.uncoverInterval = _uncoverInterval.isEmpty() ? COMMA : _uncoverInterval;
        this.unCoverPre = properties.getProperty(CONFIG.UNCOVER_PRE.getValue(), "__");
        this.winStartField = properties.getProperty(CONFIG.WIN_START.getValue());
        this.winEndField = properties.getProperty(CONFIG.WIN_END.getValue());
        String _storeName = properties.getProperty(CONFIG.WIN_STORE.getValue(), "aggregation");
        this.storeName = _storeName.isEmpty() ? "aggregation" : _storeName;
        this.addWinStart = !isNullOrEmpty(winStartField);
        this.addWinEnd = !isNullOrEmpty(winEndField);
    }

    /**
     * process impl
     * <p>
     * 处理后的value增加count,window_start,window_end三个属性列
     * 处理后的key为分组属性列组成的json字符串
     * <p>
     * 如果value中没有任何一个group key,则这条记录drop
     * 如果json转换出错,这条记录drop
     *
     * @param kSources kStream or kTable
     * @return kStream or kTable
     */
    @SuppressWarnings("unchecked")
    @Override
    Object processImpl(Object... kSources) {
        Object kSource = kSources[0];
        if (kSource instanceof KTable) {
            throw new KConfigException("kTable aggregate is not implemented temporarily...");
        } else {
            return ((KStream<String, String>) kSource).groupByKey().aggregate(() -> {
                        final ImmutableMap.Builder mapB = new ImmutableMap.Builder();
                        mapB.put(countField, "0");
                        if (!unCoverKeys.isEmpty()) unCoverKeys.forEach(k -> mapB.put(unCoverPre.concat(k), ""));
                        try {
                            return KJson.writeValueAsString(mapB.build());
                        } catch (IOException e) { //ignore
                            throw new KRunException(e);
                        }
                    }, (key, value, aggregate) -> {
                        try {
                            Map valM = KJson.readValue(value);
                            Map aggM = KJson.readValue(aggregate);
                            int pre = Integer.parseInt((String) aggM.get(countField));
                            valM.put(countField, String.valueOf(pre + 1));
                            unCoverKeys.forEach(k -> {
                                String uk = unCoverPre.concat(k);
                                Object ov = aggM.get(uk);
                                if (valM.containsKey(k)) {
                                    Object nv = valM.get(k);
                                    try {
                                        int ovi = isNullOrEmpty(ov) ? 0 : Integer.parseInt(String.valueOf(ov));
                                        int nvi = Integer.parseInt(String.valueOf(nv));
                                        valM.put(uk, String.valueOf(ovi + nvi));
                                    } catch (NumberFormatException e) {
                                        valM.put(uk, String.valueOf(ov).concat(uncoverInterval).concat(String.valueOf(nv)));
                                    }
                                } else valM.put(uk, ov); //first
                            });
                            return KJson.writeValueAsString(valM);
                        } catch (IOException | RuntimeException e) {
                            logger.error(concat(NEWLINE, "aggregate error and return value...", "value:" + value), e);
                        }
                        return value;
                    },
                    TimeWindows.of(windowMs).advanceBy(advanceMs).until(retentionMs),
                    Serdes.String(), storeName).toStream()
                    .map((key, value) -> {
                        Window windowKey = key.window();
                        try {
                            Map valM = KJson.readValue(value);
                            unCoverKeys.forEach(k -> {
                                Object _nv = valM.remove(unCoverPre.concat(k));
                                if (_nv instanceof String) {
                                    String nv = (String) _nv;
                                    if (nv.startsWith(uncoverInterval)) nv = nv.substring(1);
                                    if (!uncoverRepeat) {
                                        ImmutableSet<String> set = ImmutableSet.copyOf(split(nv, uncoverInterval));
                                        valM.put(k, concat(uncoverInterval, set.iterator()));
                                    } else valM.put(k, nv);
                                } else valM.put(k, _nv);
                            });
                            if (addWinStart) valM.put(winStartField, String.valueOf(windowKey.start()));
                            if (addWinEnd) valM.put(winEndField, String.valueOf(windowKey.end()));
                            return new KeyValue<>(key.key(), KJson.writeValueAsString(valM));
                        } catch (IOException | RuntimeException e) {
                            logger.error(value, e);
                        }
                        return new KeyValue<>(null, null);
                    });

        }
    }
}
