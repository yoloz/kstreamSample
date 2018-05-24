package com.unimas.kska.process.operation;


import com.google.common.collect.ImmutableList;
import org.apache.kafka.streams.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * kv.fields.noExist.append 如果提供的field不存在是否追加"null",默认false
 * <p>
 * key.fields 新key的组成子段,从json value中取值
 * <p>
 * kv.key.fields.type key值格式,json("fieldName":"fieldValue"),value("fieldValue").默认json,区分大小写
 * <p>
 * value.fields 新value的组成子段,从json value中取值
 */
class ConvertKVImpl extends Operation {

    private final Logger logger = LoggerFactory.getLogger(ConvertKVImpl.class);

    private final ImmutableList<String> keys;
    private final String keysType;
    private final ImmutableList<String> values;
    private boolean appendNoExist;

    ConvertKVImpl(Properties properties) {
        super(properties);
        String _appendNull = properties.getProperty("kv.fields.noExist.append", "false");
        this.appendNoExist = _appendNull.isEmpty() ? false : Boolean.valueOf(_appendNull);
        String _keys = properties.getProperty("kv.key.fields");
        this.keys = isNullOrEmpty(_keys) ? ImmutableList.of() : split(_keys, COMMA);
        String _keysType = properties.getProperty("kv.key.fields.type", "json");
        this.keysType = _keysType.isEmpty() ? "json" : _keysType;
        String _values = properties.getProperty("kv.value.fields");
        this.values = isNullOrEmpty(_values) ? ImmutableList.of() : split(_values, COMMA);
    }

    /**
     * process impl
     * <p>
     * if json value read fail,will drop
     *
     * @param kSources kStream or kTable
     * @return kStream or kTable
     */
    @Override
    Object processImpl(Object... kSources) {
        final Object kSource = kSources[0];
        if (!keys.isEmpty() || !values.isEmpty()) {
            if (!keys.isEmpty()) {//k-v,key
                return mapKeyValue(kSource, (key, value) -> {
                    try {
                        String nk = getNewKeyFromValue(key, value, keysType, keys, appendNoExist);
                        String nv = values.isEmpty() ? value : getNewValueFromValue(value, values, appendNoExist);
                        return new KeyValue<>(nk, nv);
                    } catch (IOException | RuntimeException e) {
                        logger.error(concat(NEWLINE, "convert kv error and return null...", "key:" + key, "value:" + value), e);
                    }
                    return new KeyValue<>(null, null);
                });
            } else {//value
                return mapValues(kSource, (value -> {
                    try {
                        return getNewValueFromValue(value, values, appendNoExist);
                    } catch (IOException | RuntimeException e) {
                        logger.error(concat(NEWLINE, "convert kv error and return null...", "value:" + value), e);
                    }
                    return null;
                }));
            }
        }
        return kSource;
    }
}
