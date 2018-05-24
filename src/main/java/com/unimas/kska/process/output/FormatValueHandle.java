package com.unimas.kska.process.output;

import com.google.common.collect.ImmutableList;
import com.unimas.kska.process.KUtils;
import com.unimas.kska.bean.ImmutableTriple;
import com.unimas.kska.bean.KJson;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * format.enable 数据输出前是否需要格式化
 * <p>
 * format.pattern 自定义模式,变量前后加上$且变量名不能含有双引号,如:{"columns":["数量"],"index":["$window_start$"],"data":[["$window_count$"]]}
 */
class FormatValueHandle implements KUtils {

    /**
     * configuration definition
     */
    protected enum CONFIG {
        ENABLE("format.enable"), PATTERN("format.pattern");
        private String value;

        CONFIG(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private boolean first = true;
    private final String pattern;
    //needReplace,isFieldName,content
    private ImmutableList<ImmutableTriple<Boolean, Boolean, String>> customCache;

    FormatValueHandle(Properties properties) {
        this.pattern = nonNullEmpty(properties, CONFIG.PATTERN.getValue());
    }

    /**
     * format value
     *
     * @param value value
     * @return converted value
     */
    String handle(String value) throws IOException {
        Map valM = KJson.readValue(value);
        if (first) {
            first = false;
            char doubleQuotation = '"';
            ImmutableList.Builder<ImmutableTriple<Boolean, Boolean, String>> cache = new ImmutableList.Builder<>();
            char[] valueStyles = pattern.toCharArray();
            int tmp = 0;
            for (int i = 0; i < valueStyles.length; i++) {
                char c = valueStyles[i];
                if ('$' == c) {
                    String slice = String.valueOf(Arrays.copyOfRange(valueStyles, tmp, i));
                    cache.add(ImmutableTriple.of(valM.containsKey(slice), slice.indexOf(doubleQuotation) < 0, slice));
                    tmp = i + 1;
                }
            }
            String tail = String.valueOf(Arrays.copyOfRange(valueStyles, tmp, valueStyles.length));
            cache.add(ImmutableTriple.of(valM.containsKey(tail), tail.indexOf(doubleQuotation) < 0, tail));
            customCache = cache.build();
        }
        StringBuilder stringBuilder = new StringBuilder();
        customCache.forEach(tuple -> {
            if (tuple.getLeft()) stringBuilder.append(valM.get(tuple.getRight()));
            else if (tuple.getMiddle()) stringBuilder.append("");
            else stringBuilder.append(tuple.getRight());
        });
        return stringBuilder.toString();
    }
}
