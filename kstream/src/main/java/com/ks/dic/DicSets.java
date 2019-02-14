package com.ks.dic;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.ks.error.KConfigException;
import static com.ks.process.KUtils.*;

import java.io.Closeable;
import java.util.Properties;

/**
 * dic.type kafka,array
 * <p>
 * dic.fields dic映射的字段名.如:f1,f2...
 */
public abstract class DicSets implements Runnable, Closeable {

    /**
     * configuration definition
     */
    public static final String dicType = "dic.type";

    ImmutableList<String> fields;

    DicSets(Properties properties) {
        this.fields = split(nonNullEmpty(properties, "dic.fields"), COMMA);
    }

    /**
     * 获取输出实现
     *
     * @param target target dic
     * @param conf   configuration
     * @return dicSets {@link DicSets}
     */
    public static Optional<DicSets> getImpl(String target, Properties conf) {
        if (Strings.isNullOrEmpty(target)) return Optional.absent();
        switch (target) {
            case "array":
                return Optional.of(new SimpleMap(conf));
            case "kafka":
                return Optional.of(new SkimpyTopicMap(conf));
            default:
                throw new KConfigException("dic type '" + target + "' not support...");
        }
    }

    /**
     * Returns <tt>true</tt> if this sets contains the specified element.
     * <p>
     * Returns <tt>true</tt> if this sets is empty.
     *
     * @param name  field name
     * @param value filed value
     * @return <tt>true</tt> if this sets contains the specified element
     */
    public abstract boolean contains(String name, Object value);

    /**
     * is need to block
     *
     * @return true if this need to wait
     */
    public boolean isBlock() {
        return false;
    }

    /**
     * close resources
     */
    public void close() {
    }
}
