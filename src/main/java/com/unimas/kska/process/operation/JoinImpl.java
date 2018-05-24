package com.unimas.kska.process.operation;

import com.google.common.collect.ImmutableList;
import com.unimas.kska.bean.KJson;
import com.unimas.kska.error.KConfigException;
import com.unimas.kska.error.KRunException;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * join.ks.name 执行join操作的目标kSource名称
 * <p>
 * join.source.through source的partition与target不一致,运行前手动创建一个与target相同partition的topic,{@link DefaultPartitioner} is used.
 * <p>
 * join.target.through target的partition与source不一致,运行前手动创建一个与source相同partition的topic{@link DefaultPartitioner} is used.
 * <p>
 * 如果operation.ks与join.ks两者有一者为table,则before,after,retention无需配置.
 * join.beforeMs=0
 * join.afterMs=0
 * SELECT * FROM stream1, stream2 WHERE stream1.key = stream2.key AND stream1.ts - before <= stream2.ts AND stream2.ts <= stream1.ts + after
 * There are three different window configuration supported:
 * 1,before = after = time-difference;
 * 2,before = 0 and after = time-difference;
 * 3,before = time-difference and after = 0
 * <p>
 * join.retentionMs retentionMs > before+after
 * <p>
 * join.output.strategy 输出策略,overwrite:相同子段覆盖;uncover:相同的子段后缀添加数字,默认overwrite.如f=>f1,f1=>f11...
 * <p>
 * join.output.fields.value.add 输出策略外的允许同名字段值追加,优先级高于输出策略
 * <p>
 * join.output.fields.value.add.interval 值追加间隔符.默认','
 * <p>
 * 支持以下方式：
 * KTable [join,leftJoin,outerJoin] KTable
 * KStream [join,leftJoin,outerJoin] KStream
 * KStream [join,leftJoin] KTable
 * **************************************************************
 * kafka0.11版本
 * ***************************************************
 * *****************Stream-->Stream*******************
 * *****************join*****************
 * -----------------------------------
 * this     other       result
 * -----------------------------------
 * <K1:A>
 * -----------------------------------
 * <K2:B>   <K2:b> 	<K2:ValueJoiner(B,b)>
 * ------------------------------------
 * <K3:c>
 * ------------------------------------
 * *****************leftJoin*****************
 * -----------------------------------
 * this     other       result
 * -----------------------------------
 * <K1:A>            <K1:ValueJoiner(A,null)>
 * -----------------------------------
 * ***************  <K2:ValueJoiner(B,null)>
 * <K2:B>   <K2:b> 	<K2:ValueJoiner(B,b)>
 * ------------------------------------
 * <K3:c>
 * ------------------------------------
 * ********leftJoin多出来的(B,null),可以先保存,然后以table读出来
 * *****************outerJoin*****************
 * -----------------------------------
 * this     other       result
 * -----------------------------------
 * <K1:A> 		    <K1:ValueJoiner(A,null)>
 * -----------------------------------
 * ***************  <K2:ValueJoiner(B,null)>
 * <K2:B>   <K2:b> 	<K2:ValueJoiner(B,b)>
 * ------------------------------------
 * <K3:c> 	        <K3:ValueJoiner(null,c)>
 * ------------------------------------
 * ********可以采取leftJoin的方式处理多出的(B,null)值
 * ***************************************************
 * *****************Table-->Table*****************
 * 如果两个table都在更新,则结果类似笛卡尔积
 * *****************join*****************
 * ***********需要配置store(operation.table.store),否则则(B,b)会多次输出
 * -----------------------------------
 * thisState     otherState       result
 * -----------------------------------
 * <K1:A>
 * -----------------------------------
 * <K2:B>   <K2:b> 	<K2:ValueJoiner(B,b)>
 * ------------------------------------
 * <K3:c>
 * ------------------------------------
 * *****************leftJoin*****************
 * ***********需要配置store(operation.table.store),否则则(B,b)会多次输出
 * -----------------------------------
 * thisState     otherState       result
 * -----------------------------------
 * <K1:A>            <K1:ValueJoiner(A,null)>
 * -----------------------------------
 * <K2:B>   <K2:b> 	 <K2:ValueJoiner(B,b)>
 * ------------------------------------
 * <K3:c>
 * ------------------------------------
 * *****************outerJoin*****************
 * ***********需要配置store(operation.table.store),否则则(B,b)会多次输出
 * -----------------------------------
 * thisState     otherState       result
 * -----------------------------------
 * <K1:A> 		    <K1:ValueJoiner(A,null)>
 * -----------------------------------
 * <K2:B>   <K2:b> 	<K2:ValueJoiner(B,b)>
 * ------------------------------------
 * <K3:c> 	        <K3:ValueJoiner(null,c)>
 * ------------------------------------
 * ***************************************************
 * *****************Stream-->Table*****************
 * *****************join*****************
 * **The join is a primary key table lookup join with join attribute stream.key == table.key.
 * **"Table lookup join" means, that results are only computed if KStream records are processed.
 * **In contrast, processing KTable input records will only update the internal KTable state
 * **and will not produce any result records.
 * -----------------------------------
 * this     otherState       result
 * -----------------------------------
 * <K1:A>
 * -----------------------------------
 * <K2:B>   <K2:b> 	<K2:ValueJoiner(B,b)>
 * ------------------------------------
 * <K3:c>
 * ------------------------------------
 * *****************leftJoin*****************
 * -----------------------------------
 * this     otherState       result
 * -----------------------------------
 * <K1:A>            <K1:ValueJoiner(A,null)>
 * -----------------------------------
 * <K2:B>   <K2:b> 	 <K2:ValueJoiner(B,b)>
 * ------------------------------------
 * <K3:c>
 * ------------------------------------
 */


class JoinImpl extends Operation {

    private final Logger logger = LoggerFactory.getLogger(JoinImpl.class);

    /**
     * configuration definition
     */
    private enum CONFIG {
        JOIN_KSOURCE_NAME("join.ks.name"), SOURCE_THROUGH("join.source.through"), TARGET_THROUGH("join.target.through"),
        WIN_BEFORE("join.beforeMs"), WIN_AFTER("join.afterMs"), WIN_RETENTION("join.retentionMs"),
        OUTPUT_STRATEGY("join.output.strategy"), OUTPUT_ADD_VALUE_FIELDS("join.output.fields.value.add"),
        ADD_VALUE_FIELD_INTERVAL("join.output.fields.value.add.interval");

        private String value;

        CONFIG(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final String joinKSourceName;
    private final String sourceThrough;
    private final String targetThrough;
    private final long beforeMs;
    private final long afterMs;
    private final long retentionMs;
    private final boolean isCover;
    private final ImmutableList<String> addValueFields;
    private final String addValueFieldInterval;


    JoinImpl(Properties properties) {
        super(properties);
        this.joinKSourceName = nonNullEmpty(properties, CONFIG.JOIN_KSOURCE_NAME.getValue());
        this.sourceThrough = properties.getProperty(CONFIG.SOURCE_THROUGH.getValue());
        this.targetThrough = properties.getProperty(CONFIG.TARGET_THROUGH.getValue());
        String _beforeMs = properties.getProperty(CONFIG.WIN_BEFORE.getValue(), "0");
        this.beforeMs = _beforeMs.isEmpty() ? 0L : Long.valueOf(_beforeMs);
        String _afterMs = properties.getProperty(CONFIG.WIN_AFTER.getValue(), "0");
        this.afterMs = _afterMs.isEmpty() ? 0L : Long.valueOf(_afterMs);
        String _retentionMs = properties.getProperty(CONFIG.WIN_RETENTION.getValue(), "1");
        long retentionMs = _retentionMs.isEmpty() ? 1L : Long.valueOf(_retentionMs);
        if (retentionMs < beforeMs + afterMs) {
            logger.warn("join window retention time cannot be smaller than before and after sum.");
            this.retentionMs = (beforeMs + afterMs) + 1;
        } else this.retentionMs = retentionMs;
        String _outputStrategy = properties.getProperty(CONFIG.OUTPUT_STRATEGY.getValue(), "cover");
        _outputStrategy = _outputStrategy.isEmpty() ? "cover" : _outputStrategy;
        switch (_outputStrategy) {
            case "uncover":
                this.isCover = false;
                break;
            case "cover":
                this.isCover = true;
                break;
            default:
                logger.warn(concat(" ", CONFIG.OUTPUT_STRATEGY.getValue(), "value:", _outputStrategy,
                        "not support..."));
                this.isCover = true;
        }
        String _addValueFields = properties.getProperty(CONFIG.OUTPUT_ADD_VALUE_FIELDS.getValue());
        this.addValueFields = isNullOrEmpty(_addValueFields) ? ImmutableList.of() : split(_addValueFields, COMMA);
        String _addValueFieldInterval = properties.getProperty(CONFIG.ADD_VALUE_FIELD_INTERVAL.getValue(), COMMA);
        this.addValueFieldInterval = _addValueFieldInterval.isEmpty() ? COMMA : _addValueFieldInterval;
    }

    /**
     * process impl
     * <p>
     * if json value read fail,will drop
     * 支持以下方式：
     * <p>
     * KTable [join,leftJoin,outerJoin] KTable
     * <p>
     * KStream [join,leftJoin,outerJoin] KStream
     * <p>
     * KStream [join,leftJoin] KTable
     *
     * @param kSources kStream or kTable
     * @return kStream or kTable
     */
    @SuppressWarnings("unchecked")
    @Override
    Object processImpl(Object... kSources) {
        Object source = kSources[0], other = kSources[1];
        if (!isNullOrEmpty(sourceThrough)) source = through(source, sourceThrough);
        if (!isNullOrEmpty(targetThrough)) other = through(other, targetThrough);
        Class clazz = source.getClass();
        Method handle;
        final ValueJoiner<String, String, String> joiner = (value1, value2) -> {
            try {
                return mergeValue(value1, value2);
            } catch (IOException | RuntimeException e) {
                logger.error(concat(NEWLINE, "merge value error and return value...", "value:" + value1,
                        "otherValue:" + value2), e);
            }
            return value1;
        };
        try {
            if (source instanceof KTable) {
                if ((other instanceof KTable)) {// table--table
                    if (!tableStoreName.isEmpty()) {
                        handle = clazz.getDeclaredMethod(_operator,
                                KTable.class, ValueJoiner.class, Serde.class, String.class);
                        return handle.invoke(source, other, joiner, Serdes.String(), tableStoreName);
                    } else {
                        handle = clazz.getDeclaredMethod(_operator, KTable.class, ValueJoiner.class);
                        return handle.invoke(source, other, joiner);
                    }
                } else throw new KConfigException(concat(" ",
                        kSourceName + " is table and", joinKSourceName, "is not table,any joins can not support..."));
            }
            if (other instanceof KTable) { //stream--table
                if ("outerJoin".equals(_operator)) throw new KConfigException(concat(" ", kSourceName,
                        "is stream and", joinKSourceName, "is table,outerJoin can not support..."));
                handle = clazz.getDeclaredMethod(_operator, KTable.class, ValueJoiner.class, Serde.class, Serde.class);
                return handle.invoke(source, other, joiner, Serdes.String(), Serdes.String());
            }
            //stream--stream
            handle = clazz.getDeclaredMethod(_operator, KStream.class, ValueJoiner.class, JoinWindows.class,
                    Serde.class, Serde.class, Serde.class);
            return handle.invoke(source, other, joiner, JoinWindows.of(beforeMs).after(afterMs).until(retentionMs),
                    Serdes.String(), Serdes.String(), Serdes.String());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.error(e.getMessage(), e);
            throw new KRunException(e);
        }
    }

    /**
     * process for join
     */
    @Override
    public void process(Map<String, Object> kSources) {
        if (!kSources.containsKey(kSourceName) || !kSources.containsKey(joinKSourceName))
            throw new KConfigException(concat(" ", kSourceName, "or", joinKSourceName, "does not exit..."));
        kSources.put(kSourceName, processImpl(kSources.get(kSourceName), kSources.get(joinKSourceName)));
    }

    /**
     * merge value and filter by specific fields
     * <p>
     * 如果有同名key,则优先是否值追加,相同值只保留一个,不同值间隔符分隔
     * <p>
     * 如果有重复key且是不覆盖,则会输出key,key1,即重复key后面添加“1”
     *
     * @param value      json value
     * @param otherValue json value
     * @return json value
     */
    @SuppressWarnings("unchecked")
    private String mergeValue(String value, String otherValue) throws IOException {
        Map<String, Object> valM = isNullOrEmpty(value) ? Collections.emptyMap() :
                KJson.readValue(value);
        Map<String, Object> otherValM = isNullOrEmpty(otherValue) ? Collections.emptyMap() :
                KJson.readValue(otherValue);
        Map<String, Object> resultMap = new HashMap<>(valM.size() + otherValM.size());
        resultMap.putAll(valM);
        otherValM.forEach((k, v) -> {
            if (valM.containsKey(k)) {
                if (addValueFields.contains(k)) {
                    Object v1 = valM.get(k), v2 = otherValM.get(k);
                    Object val = Objects.equals(v1, v2) ? v1 : String.valueOf(v1) +
                            addValueFieldInterval + v2;
                    resultMap.put(k, val);
                } else if (!isCover) {
                    resultMap.put(k + 1, v);
                } else {
                    resultMap.put(k, v);
                }
            } else resultMap.put(k, v);
        });
        return KJson.writeValueAsString(resultMap);
    }
}
