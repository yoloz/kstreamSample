package com.ks.process.operation;

import com.google.common.collect.ImmutableList;
import com.ks.bean.KJson;
import com.ks.dic.DicSets;
import com.ks.error.KConfigException;
import com.ks.process.AppImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * filter.conds 过滤字段操作暂支持(in,notin).如:in,notIn...
 * <p>
 * filter.fields 过滤字段,与actions对应.如:k1,k2...
 */
class FilterImpl extends Operation {

    private final Logger logger = LoggerFactory.getLogger(FilterImpl.class);

    private ImmutableList<String> conds;
    private ImmutableList<String> fields;

    FilterImpl(Properties properties) {
        super(properties);
        this.conds = split(nonNullEmpty(properties, "filter.conds"), COMMA);
        this.fields = split(nonNullEmpty(properties, "filter.fields"), COMMA);
        if (conds.size() != fields.size()) throw new KConfigException("filter conds,fields size is not equal...");

    }

    /**
     * process impl
     * 如果value值格式化出错,则filter无效,返回true
     * <p>
     * 如果action类型暂不支持,则filter无效,返回true
     *
     * @param kSources kStream or kTable
     * @return kStream or kTable
     */
    @Override
    Object processImpl(Object... kSources) {
        final DicSets dicSets = AppImpl.getDicSets();
        return filter(kSources[0], (k, v) -> {
            try {
                Map valM = KJson.readValue(v);
                boolean result = true;
                for (int i = 0; i < fields.size(); i++) {
                    String filedName = fields.get(i);
                    String cond = conds.get(i);
                    if (valM.containsKey(filedName)) {
                        switch (cond) {
                            case "in":
                                result = result && dicSets.contains(filedName, valM.get(filedName));
                                break;
                            case "notIn":
                                result = result && !dicSets.contains(filedName, valM.get(filedName));
                                break;
                            default:
                                throw new KConfigException(concat(" ", "filter cond",
                                        cond, "not support..."));
                        }
                    }
                }
                return result;
            } catch (IOException | RuntimeException e) {
                logger.error(v, e);
            }
            return true;
        });
    }
}
