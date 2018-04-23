package com.unimas.kstream.process.output;

import com.google.common.collect.ImmutableList;
import com.unimas.kstream.error.KConfigException;
import com.unimas.kstream.process.KUtils;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.stream.IntStream;

/**
 * output.fields 输出字段,如:f1,f2,f3...不配置即默认所有.注意：如果配置的f不存在,输出会增加f,值为"null"
 * <p>
 * output.fields.noExist.append 如果提供的field不存在是否追加"null",默认false
 * <p>
 * output.targets kafka,zbus,sysout...默认sysout.注意区分大小写
 * <p>
 * output.target.kafka.address 目标源kafka地址,未配置则使用流地址
 * <p>
 * output.target.kafka.topic 目标源kafka,允许自动创建或者topic已存在
 * <p>
 * output.target.zbus.address 目标源zbus地址
 * <p>
 * output.target.zbus.mq 目标源zbus mq
 */
public abstract class OutPut implements KUtils, Closeable {

    private final Logger logger = LoggerFactory.getLogger(OutPut.class);

    /**
     * configuration definition
     */
    public enum CONFIG {
        OUT_KSOURCE("output.ks.name"), OUT_KEYS("output.fields"),
        APPEND_NOT_EXIST("output.fields.noExist.append"),
        //        OUT_STYLE("output.style"), OUT_CUSTOM("output.custom"),
        OUT_TARGETS("output.targets"), OUT_KAFKA_ADDRESS("output.target.kafka.address");
        private String value;

        CONFIG(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    private final String outKSourceName;
    private ImmutableList<String> outputFields;
    private final boolean appendNoExist;
    private final boolean enableExpandWin;
    private final ExpandWinHandle expandWinHandle;
    private final boolean enableFormatValue;
    private final FormatValueHandle formatValueHandle;


    OutPut(Properties properties) {
        this.outKSourceName = nonNullEmpty(properties, CONFIG.OUT_KSOURCE.getValue());
        String output_fields = properties.getProperty(CONFIG.OUT_KEYS.getValue());
        this.outputFields = isNullOrEmpty(output_fields) ? ImmutableList.of() : split(output_fields, COMMA);
        String _appendNoExist = properties.getProperty(CONFIG.APPEND_NOT_EXIST.getValue(), "false");
        this.appendNoExist = _appendNoExist.isEmpty() ? false : Boolean.valueOf(_appendNoExist);
        String _enableExpandWin = properties.getProperty(ExpandWinHandle.CONFIG.ENABLE.getValue(), "false");
        this.enableExpandWin = _enableExpandWin.isEmpty() ? false : Boolean.valueOf(_enableExpandWin);
        this.expandWinHandle = this.enableExpandWin ? new ExpandWinHandle(properties) : null;
        String _enableFormat = properties.getProperty(FormatValueHandle.CONFIG.ENABLE.getValue(), "false");
        this.enableFormatValue = _enableFormat.isEmpty() ? false : Boolean.valueOf(_enableFormat);
        this.formatValueHandle = this.enableFormatValue ? new FormatValueHandle(properties) : null;
    }


    private void outputImpl(String key, String value, OutPut... outPuts) {
        try {
            if (enableFormatValue) value = formatValueHandle.handle(value);
            for (OutPut outPut : outPuts) {
                outPut.apply(key, value);
            }
        } catch (IOException | RuntimeException e) {
            logger.error(concat(NEWLINE, "json format error", "value:" + value), e);
        }
    }

    /**
     * 输出到目标库
     *
     * @param kStream 数据流
     */
    @SuppressWarnings("unchecked")
    public void output(KStream<String, String> kStream, OutPut... outPuts) {
        if (outPuts == null || outPuts.length == 0) return;
        kStream.foreach((k, v) -> {
            try {
                if (!outputFields.isEmpty()) v = getNewValueFromValue(v, outputFields, appendNoExist);
                if (enableExpandWin) expandWinHandle.handle(k, v, (nk, nv) -> outputImpl(nk, nv, outPuts));
                else outputImpl(k, v, outPuts);
            } catch (IOException | RuntimeException e) {
                logger.error(concat(NEWLINE, "json format error", "value:" + v), e);
            }
        });
    }

    /**
     * 获取输出实现
     *
     * @param conf    output configuration
     * @param targets target type
     * @return outPut {@link OutPut}
     */
    public static OutPut[] getImpl(Properties conf, Object... targets) {
        if (targets == null || targets.length == 0) throw new KConfigException(" outPut target is null or empty...");
        OutPut[] outPuts = new OutPut[targets.length];
        IntStream.range(0, targets.length).forEach(i -> {
            String target = (String) targets[i];
            switch (target) {
                case "kafka":
                    outPuts[i] = new KafkaOutput(conf);
                    break;
                case "zbus":
                    outPuts[i] = new ZbusOutput(conf);
                    break;
                case "sysout":
                    outPuts[i] = new SysOut(conf);
                    break;
                default:
                    throw new KConfigException("output target'" + target + "'not support...");
            }
        });
        return outPuts;
    }

    public String getOutKSourceName() {
        return outKSourceName;
    }

    /**
     * 输出到目标库
     *
     * @param key   key
     * @param value value
     */
    abstract void apply(String key, String value);

    /**
     * close expandWin
     */
    public void closeExpandWin() {
        if (enableExpandWin) expandWinHandle.close();
    }

    /**
     * close resources
     */
    public void close() {
    }
}
