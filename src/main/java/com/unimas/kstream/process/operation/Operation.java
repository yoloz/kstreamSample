package com.unimas.kstream.process.operation;

import com.unimas.kstream.process.KUtils;
import com.unimas.kstream.error.KConfigException;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.util.Map;
import java.util.Properties;

/**
 * operation.operator
 * <p>
 * operation.ks.name 执行操作的kSource名称
 * <p>
 * operation.table.store 如果操作的kSource是table,提供自定义的storeName,会生成topic.
 * table-table建议配置
 * stream-stream,stream-table不需要这个配置
 */
public abstract class Operation implements KUtils {

    public final static String operator = "operation.operator";
    public final static String operatorKS = "operation.ks.name";

    final String kSourceName;
    final String _operator;
    final String tableStoreName;


    Operation(Properties properties) {
        this._operator = nonNullEmpty(properties, operator);
        this.kSourceName = nonNullEmpty(properties, operatorKS);
        this.tableStoreName = properties.getProperty("operation.table.store", "");
    }


    /**
     * 获取输出实现
     *
     * @param target operator
     * @param conf   operation configuration
     * @return operation {@link Operation}
     */
    public static Operation getImpl(String target, Properties conf) {
        switch (target) {
            case "filter":
                return new FilterImpl(conf);
            case "mapper":
                return new MapperImpl(conf);
            case "window":
                return new WinAggImpl(conf);
            case "join":
            case "leftJoin":
            case "outerJoin":
                return new JoinImpl(conf);
            case "convertTime":
                return new ConvertTimeImpl(conf);
            case "convertKV":
                return new ConvertKVImpl(conf);
            default:
                throw new KConfigException("operation operator '" + target + "' not support...");
        }
    }

    /**
     * mapper value
     *
     * @param object      kStream,kTable
     * @param valueMapper valueMapper {@link ValueMapper}
     * @return kStream or kTable
     */
    @SuppressWarnings("unchecked")
    Object mapValues(Object object, ValueMapper<String, String> valueMapper) {
        if (object instanceof KTable) {
            if (!tableStoreName.isEmpty()) {
                return ((KTable<String, String>) object).mapValues(valueMapper, Serdes.String(), tableStoreName);
            } else return ((KTable<String, String>) object).mapValues(valueMapper);
        } else return ((KStream<String, String>) object).mapValues(valueMapper);
    }

    /**
     * mapper key or mapper K-V
     *
     * @param object         kStream or kTable
     * @param keyValueMapper keyValueMapper {@link KeyValueMapper}
     * @return kStream or kTable
     */
    @SuppressWarnings("unchecked")
    Object mapKeyValue(Object object, KeyValueMapper<String, String, KeyValue<String, String>> keyValueMapper) {
        if (object instanceof KTable) object = ((KTable<String, String>) object).toStream();
        return ((KStream<String, String>) object).map(keyValueMapper);
    }

    /**
     * Materialize this changelog stream to a topic and creates a new {@code KTable} from the topic.
     * The specified topic should be manually created before it is used (i.e., before the Kafka Streams application is
     * started).
     *
     * @param object       kStream or kTable
     * @param throughTopic through topic name
     * @return kStream or kTable
     */
    @SuppressWarnings("unchecked")
    Object through(Object object, String throughTopic) {
        if (object instanceof KTable)
            object = ((KTable<String, String>) object).through(Serdes.String(), Serdes.String(), throughTopic);
        return ((KStream<String, String>) object).through(Serdes.String(), Serdes.String(), throughTopic);
    }

    /**
     * filter value
     *
     * @param object    kStream or kTable
     * @param predicate predicate {@link Predicate}
     * @return kStream or kTable
     */
    @SuppressWarnings("unchecked")
    Object filter(Object object, Predicate<String, String> predicate) {
        if (object instanceof KTable) {
            if (!tableStoreName.isEmpty()) {
                return ((KTable<String, String>) object).filter(predicate, tableStoreName);
            } else return ((KTable<String, String>) object).filter(predicate);
        } else return ((KStream<String, String>) object).filter(predicate);
    }


    /**
     * default process for one kSource
     * <p>
     * otherwise,multi kSource you need to realize it.like join operation...
     */
    public void process(Map<String, Object> kSources) {
        if (!kSources.containsKey(kSourceName))
            throw new KConfigException(concat(" ", kSourceName, "does not exit..."));
        kSources.put(kSourceName, processImpl(kSources.get(kSourceName)));
    }

    /**
     * process impl
     *
     * @param kSources kStream or kTable
     * @return kStream or kTable
     */
    abstract Object processImpl(Object... kSources);

}
