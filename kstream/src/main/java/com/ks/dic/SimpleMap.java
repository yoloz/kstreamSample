package com.ks.dic;

import com.google.common.collect.ImmutableList;
import com.ks.error.KConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 简单的配置分组存储
 * can be used for test
 * <p>
 * dic.array.values 存储type是array的多组值,如：f11,f12,f13...;f21,f22,f23...
 */
class SimpleMap extends DicSets {

    private final Logger logger = LoggerFactory.getLogger(SimpleMap.class);

    private ImmutableList<ImmutableList<String>> arr_values;
    private ConcurrentMap<String, ImmutableList<String>> cache;

    SimpleMap(Properties properties) {
        super(properties);
        String _array = nonNullEmpty(properties, "dic.array.values");
        ImmutableList.Builder<ImmutableList<String>> builder = new ImmutableList.Builder<>();
        ImmutableList<String> il = split(_array, SEMICOLON);
        il.forEach(s -> builder.add(split(s, COMMA)));
        this.arr_values = builder.build();
        if (fields.size() != arr_values.size())
            throw new KConfigException("dic fields length and array value length is not equal...");
        this.cache = new ConcurrentHashMap<>();
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
    @Override
    public boolean contains(String name, Object value) {
        if (cache.isEmpty()) {
            logger.warn("dic sets is empty......result set always true");
            return true;
        } else {
            return cache.containsKey(name) && cache.get(name).contains(String.valueOf(value));
        }
    }

    /**
     * is need to block
     *
     * @return true if this need to wait
     */
    @Override
    public boolean isBlock() {
        return false;
    }

    /**
     * close impl
     */
    @Override
    public void close() {

    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        for (int i = 0; i < fields.size(); i++) {
            String key = fields.get(i);
            this.cache.put(key, arr_values.get(i));
        }
    }
}
