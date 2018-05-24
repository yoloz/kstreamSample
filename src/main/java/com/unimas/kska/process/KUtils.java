package com.unimas.kska.process;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.unimas.kska.bean.KJson;
import com.unimas.kska.error.KConfigException;

import java.io.IOException;
import java.util.*;

/**
 * utils
 */
public interface KUtils {

    String NEWLINE = System.getProperty("line.separator", "\n");
    String COMMA = ",";
    String SEMICOLON = ";";
    String COLON = ":";

    /**
     * get value own specific fields
     *
     * @param value  json value
     * @param terms  specific fields
     * @param append is it needed to add "null" if it does not exit
     * @return json value
     */
    @SuppressWarnings("unchecked")
    default String getNewValueFromValue(String value, List<String> terms, boolean append)
            throws IOException {
        if (isNullOrEmpty(value) || terms == null || terms.isEmpty()) return value;
        Map valM = KJson.readValue(value);
        Map<String, Object> result = new HashMap<>(terms.size());
        terms.forEach(f -> {
            if (valM.containsKey(f)) result.put(f, valM.get(f));
            else if (append) result.put(f, "null");
        });
        return KJson.writeValueAsString(result);
    }

    /**
     * get key own specific fields
     *
     * @param key    key
     * @param value  json value
     * @param terms  specific field
     * @param append is it needed to add "null" if it does not exit
     * @return json key
     * @throws IOException if json value read fail
     */
    default String getNewKeyFromValue(String key, String value, String keyType, List<String> terms,
                                      boolean append) throws IOException {
        if (isNullOrEmpty(value) || terms == null || terms.isEmpty()) return key;
        final Map valM = KJson.readValue(value);
        switch (keyType) {
            case "json":
                Map<String, Object> keyM = new HashMap<>(terms.size());
                terms.forEach(k -> {
                    if (valM.containsKey(k)) keyM.put(k, valM.get(k));
                    else if (append) keyM.put(k, "null");
                });
                return KJson.writeValueAsString(keyM);
            case "value":
                StringBuilder builder = new StringBuilder(terms.size());
                terms.forEach(k -> {
                    if (valM.containsKey(k)) builder.append(String.valueOf(valM.get(k))).append(COMMA);
                    else if (append) builder.append("null").append(COMMA);
                });
                String newKey = builder.toString();
                return newKey.isEmpty() ? null : newKey.substring(0, newKey.length() - 1);
            default:
                throw new KConfigException(concat(" ", keyType + " not support..."));
        }
    }

    /**
     * value is not null and empty,else throw {@link KConfigException}
     *
     * @param properties values {@link Properties}
     * @param key        kv-key
     * @return value
     * @throws KConfigException if value is null or empty
     */
    default String nonNullEmpty(Properties properties, String key) throws KConfigException {
        String value = properties.getProperty(key);
        if (isNullOrEmpty(value)) throw new KConfigException(key + " is empty...");
        return value;
    }

    /**
     * split string by separator
     *
     * @param str       string to be treated
     * @param separator the literal, nonempty string to recognize as a separator
     * @return immutable list {@link ImmutableList}
     */
    default ImmutableList<String> split(final String str, final String separator) {
        if (isNullOrEmpty(str)) return ImmutableList.of();
        return ImmutableList.copyOf(Splitter.on(separator).omitEmptyStrings().trimResults().split(str));
    }


    /**
     * concat string by separator
     *
     * @param spacer a spacer
     * @param str    string[] to be treated
     * @return string {@link String}
     */
    default String concat(final String spacer, String... str) {
        return Joiner.on(spacer).skipNulls().join(str);
    }

    /**
     * concat string by separator
     *
     * @param spacer   a spacer
     * @param iterator string[] to be treated
     * @return string {@link String}
     */
    default String concat(final String spacer, Iterator<String> iterator) {
        return Joiner.on(spacer).skipNulls().join(iterator);
    }

    /**
     * if obj is string,null or empty return true
     * otherwise,null return true
     *
     * @param obj obj
     * @return true/false
     */
    default boolean isNullOrEmpty(Object obj) {
        if (obj instanceof String) return Strings.isNullOrEmpty((String) obj);
        else return Objects.isNull(obj);
    }

    /**
     * reduce instanceof
     *
     * @param obj string
     * @return true/false
     */
    default boolean isNullOrEmpty(String obj) {
        return Strings.isNullOrEmpty(obj);
    }
}
