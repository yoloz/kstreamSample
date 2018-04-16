package com.unimas.kstream.process.operation;

import com.google.common.collect.ImmutableList;
import com.unimas.kstream.bean.KJson;
import com.unimas.kstream.bean.KTime;
import com.unimas.kstream.error.KConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * ********************************************
 * time.in.names 输入时间字段.如:f1,f2,f3...
 * <p>
 * time.in.value.types 时间值类型,long(unix时间戳),string.与输入字段对应
 * <p>
 * time.in.formats long值无需配置format.
 * 注意：long,string同时存在,format对应的long要添加半角逗号.如[long,string]==>[,uuuu-MM-dd HH:mm:ss.SSS]
 * <p>
 * time.in.lang 时间值值的语言,默认en
 * <p>
 * time.in.offsetId 时间值的时区,默认东八区.注意:如果值类似yyyy-MM-dd'T'HH:mm:ss.SSSZ,则offsetId需配置为零时区(+00:00)
 * ********************************************
 * time.out.names 输出时间字段.如:o1,o2,o3...与输入字段对应.不配置即默认输入字段
 * <p>
 * time.out.formats 输出样式.与输入字段对应.如:uuuu-MM-dd HH:mm:ss.SSS
 * 注意:如果只配置单一值,则统一输出此格式;如果不配置则输出unix时间戳
 * <p>
 * time.out.lang 时间值值的语言,默认en
 * <p>
 * time.out.offsetId 时间值的时区,默认东八区.注意:如果值类似yyyy-MM-dd'T'HH:mm:ss.SSSZ,则offsetId需配置为零时区(+00:00)
 * <p>
 * time.error.out 出现异常输出,未配置则会中断运行
 * <p>
 * time.out.type 默认输出string,kafka数据现在只处理string
 */
class ConvertTimeImpl extends Operation {

    private final Logger logger = LoggerFactory.getLogger(ConvertTimeImpl.class);

    /**
     * configuration definition
     */
    private enum CONFIG {
        IN_NAMES("time.in.names"), IN_TYPES("time.in.value.types"), IN_FORMATS("time.in.formats"),
        IN_LANG("time.in.lang"), IN_OFFSET_ID("time.in.offsetId"), OUT_NAMES("time.out.names"),
        OUT_FORMATS("time.out.formats"), OUT_LANG("time.out.lang"), OUT_OFFSET_ID("time.out.offsetId"),
        ERROR_OUT("time.error.out");
        private String value;

        CONFIG(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private ImmutableList<String> inNames;
    private ImmutableList<String> inTypes;
    private ImmutableList<String> inFormats;
    private final String inLang;
    private final String inOffsetId;
    private ImmutableList<String> outNames;
    private ImmutableList<String> outFormats;
    private final String outLang;
    private final String outOffsetId;
    private final String errorOut;

    ConvertTimeImpl(Properties properties) {
        super(properties);
        this.check(properties);
        this.inLang = properties.getProperty(CONFIG.IN_LANG.getValue());
        this.inOffsetId = properties.getProperty(CONFIG.IN_OFFSET_ID.getValue());
        this.outLang = properties.getProperty(CONFIG.OUT_LANG.getValue());
        this.outOffsetId = properties.getProperty(CONFIG.OUT_OFFSET_ID.getValue());
        this.errorOut = properties.getProperty(CONFIG.ERROR_OUT.getValue());
    }

    /**
     * process impl
     * <p>
     * 如果转换出错,输出错误常量,否则中断
     *
     * @param kSources kStream or kTable
     * @return kStream or kTable
     */
    @SuppressWarnings("unchecked")
    @Override
    Object processImpl(Object... kSources) {
        ImmutableList.Builder<KTime> inBuilder = ImmutableList.builder();
        ImmutableList.Builder<KTime> outBuilder = ImmutableList.builder();
        for (int i = 0; i < inNames.size(); i++) {
            KTime.Builder ib = new KTime.Builder().name(inNames.get(i)).type(inTypes.get(i));
            if (!inFormats.isEmpty()) ib.format(inFormats.get(i));
            ib.lang(inLang).offsetId(inOffsetId);
            inBuilder.add(ib.build().get()); //always present
            KTime.Builder ob = new KTime.Builder().name(outNames.get(i));
            if (!outFormats.isEmpty()) {
                if (outFormats.size() == 1) ob.format(outFormats.get(0));
                else ob.format(outFormats.get(i));
            }
            ob.lang(outLang).offsetId(outOffsetId);
            outBuilder.add(ob.build().get());//always present
        }
        ImmutableList<KTime> source = inBuilder.build();
        ImmutableList<KTime> target = outBuilder.build();
        return mapValues(kSources[0], (value -> {
            try {
                Map valM = KJson.readValue(value);
                for (int i = 0; i < source.size(); i++) {
                    KTime s = source.get(i), t = target.get(i);
                    String sn = s.getName();
                    if (valM.containsKey(sn)) {
                        String tv;
                        try {
                            tv = s.convert(String.valueOf(valM.remove(sn)), t);
                        } catch (Throwable e) {
                            logger.warn(concat("==>", value, sn), e);
                            if (!isNullOrEmpty(errorOut)) tv = errorOut;
                            else throw e;
                        }
                        valM.put(t.getName(), tv);
                    }
                }
                return KJson.writeValueAsString(valM);
            } catch (IOException | RuntimeException e) {
                logger.error(value, e);
            }
            return null;
        }));
    }

    /**
     * check configuration
     *
     * @param properties conf
     */
    private void check(Properties properties) {
        String in_names = nonNullEmpty(properties, CONFIG.IN_NAMES.getValue());
        inNames = ImmutableList.copyOf(split(in_names, COMMA));
        String in_types = nonNullEmpty(properties, CONFIG.IN_TYPES.getValue());
        if (in_types.contains(KTime.Type.STRING.getValue())) {
            inFormats = ImmutableList.copyOf(
                    split(nonNullEmpty(properties, CONFIG.IN_FORMATS.getValue()), COMMA));
        } else inFormats = ImmutableList.of();
        inTypes = ImmutableList.copyOf(split(in_types, COMMA));
        if (inNames.size() != inTypes.size()) throw new KConfigException(concat(" ",
                CONFIG.IN_NAMES.getValue(), "size not equal", CONFIG.IN_TYPES.getValue(), "size..."));
        if (!inFormats.isEmpty() && inNames.size() != inFormats.size())
            throw new KConfigException(concat(" ", CONFIG.IN_NAMES.getValue(),
                    "size not equal", CONFIG.IN_FORMATS.getValue(), "size..."));
        String out_names = properties.getProperty(CONFIG.OUT_NAMES.getValue(), in_names);
        out_names = out_names.isEmpty() ? in_names : out_names;
        outNames = ImmutableList.copyOf(split(out_names, COMMA));
        String out_formats = properties.getProperty(CONFIG.OUT_FORMATS.getValue());
        if (!isNullOrEmpty(out_formats)) {
            outFormats = ImmutableList.copyOf(out_formats.split(COMMA));
        } else outFormats = ImmutableList.of();
        if (outFormats.size() > 1 && inNames.size() != outFormats.size())
            throw new KConfigException(concat(" ", CONFIG.IN_NAMES.getValue()
                    , "size not equal", CONFIG.OUT_FORMATS.getValue(), "size..."));
    }
}
