package com.unimas.kstream.process.operation;

import com.google.common.collect.ImmutableList;
import com.unimas.kstream.process.KServerImpl;
import com.unimas.kstream.bean.KJson;
import com.unimas.kstream.error.KConfigException;
import com.unimas.kstream.dic.DicSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 注意：mapper操作后慎重选择window操作,可能mapper添加标签的记录会被window操作覆盖.可以window操作中配置uncover字段.
 * <p>
 * mapper.conds 转换映射操作暂支持(in,notin)如:in,notIn,in;notIn,in...
 * <p>
 * mapper.fields 转换映射字段如:k1,k2,k3;j1,j2....
 * <p>
 * mapper.appends 打标签之类功能,输出value中添加满足条件的KV对.注意：如果配置输出字段,则添加的kv需要配置.如:k1:v1;k2:v2...
 */
class MapperImpl extends Operation {

    private final Logger logger = LoggerFactory.getLogger(MapperImpl.class);

    private ImmutableList<ImmutableList<String>> conds;
    private ImmutableList<ImmutableList<String>> fields;
    private ImmutableList<String> appends;

    MapperImpl(Properties properties) {
        super(properties);
        String _conds = nonNullEmpty(properties, "mapper.conds");
        String _keys = nonNullEmpty(properties, "mapper.fields");
        this.appends = split(nonNullEmpty(properties, "mapper.appends"), SEMICOLON);
        ImmutableList<String> _conds_ = split(_conds, SEMICOLON);
        ImmutableList.Builder<ImmutableList<String>> condB = new ImmutableList.Builder<>();
        _conds_.forEach(s -> condB.add(split(s, COMMA)));
        this.conds = condB.build();
        ImmutableList<String> _keys_ = split(_keys, COMMA);
        ImmutableList.Builder<ImmutableList<String>> keyB = new ImmutableList.Builder<>();
        _keys_.forEach(s -> keyB.add(split(s, COMMA)));
        this.fields = keyB.build();
        if (conds.size() != fields.size() || conds.size() != appends.size()) {
            throw new KConfigException("mapper conds,fields,appends size is not equal...");
        }
    }

    /**
     * process impl
     * <p>
     * 如果json转换出错,这条记录drop
     * <p>
     * 映射转换后value添加配置键值对
     *
     * @param kSources kStream or kTable
     * @return kStream or kTable
     */
    @SuppressWarnings("unchecked")
    @Override
    Object processImpl(Object... kSources) {
        final DicSets dicSets = KServerImpl.getDicSets();
        return mapValues(kSources[0], (value -> {
            try {
                Map valM = KJson.readValue(value);
                Map<String, String> appendTag = new HashMap<>();
                for (int i = 0; i < appends.size(); i++) {
                    ImmutableList<String> _conds = conds.get(i);
                    ImmutableList<String> _keys = fields.get(i);
                    boolean result = true;
                    for (int j = 0; j < _keys.size(); j++) {
                        String filedName = _keys.get(j);
                        String cond = _conds.get(j);
                        if (valM.containsKey(filedName)) {
                            switch (cond) {
                                case "in":
                                    result = result && dicSets.contains(filedName, valM.get(filedName));
                                    break;
                                case "notIn":
                                    result = result && !dicSets.contains(filedName, valM.get(filedName));
                                    break;
                                default:
                                    throw new KConfigException(concat(" ", "mapper cond",
                                            cond, "not support..."));
                            }
                        }
                    }
                    if (result) {
                        ImmutableList<String> kv = split(appends.get(i), COLON);
                        if (kv.size() <= 1) {
                            throw new KConfigException(concat(" ", "mapper append value",
                                    appends.get(i), "not k-v pairs"));
                        }
                        appendTag.put(kv.get(0), kv.get(1));
                    }
                }
                valM.putAll(appendTag);
                return KJson.writeValueAsString(valM);
            } catch (IOException | RuntimeException e) {
                logger.error(value, e);
            }
            return null;
        }));
    }
}
