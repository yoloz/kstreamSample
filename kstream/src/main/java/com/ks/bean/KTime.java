package com.ks.bean;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.ks.error.KConfigException;
import com.ks.error.KRunException;
import static com.ks.process.KUtils.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Objects;


/**
 * name 时间字段名称
 * <p>
 * type 时间值类型,对于输出时间情形,kafka数据现在只处理string
 * <p>
 * offsetId offsetDateTime
 * <p>
 * formatter 输入全是long数据;或者全输出long数据
 */
public class KTime {

    /**
     * time value type
     */
    public enum Type {
        LONG("long"), STRING("string");
        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final String name;
    private final Type type;
    private final String offsetId;
    private final DateTimeFormatter formatter;

    private KTime(String name, String type, String format, String lang, String offsetId) {
        this.name = name;
        if (!isNullOrEmpty(type)) {
            if (Type.LONG.getValue().equals(type)) this.type = Type.LONG;
            else if (Type.STRING.getValue().equals(type)) this.type = Type.STRING;
            else throw new KConfigException(concat(" ", "time type", type, "not support..."));
            if (type.equals(Type.STRING.getValue()) && isNullOrEmpty(format)) {
                throw new KConfigException(concat(" ", "time field", name, "and type is",
                        type, " format is not configured..."));
            }
        } else this.type = Type.STRING; //time.out
        this.offsetId = isNullOrEmpty(offsetId) ? "+08:00" : offsetId;
        lang = isNullOrEmpty(lang) ? "en" : lang;
        if (!isNullOrEmpty(format)) {
            this.formatter = new DateTimeFormatterBuilder()
                    .appendPattern(format)
                    .toFormatter(new Locale(Objects.toString(lang, "en")));
        } else this.formatter = null;
    }

    public String getName() {
        return name;
    }

    /**
     * A builder for creating KTime instances
     * note that return optional {@link Optional}
     * <p>
     * format like 'uuuu-MM-dd'T'HH:mm:ss.SSSX'
     * <p>
     * lang language like 'en','zh'
     */
    public static final class Builder {
        private String name;
        private String type;
        private String format;
        private String lang;
        private String offsetId;

        public Optional<KTime> build() {
            if (Strings.isNullOrEmpty(name)) return Optional.absent();
            return Optional.of(new KTime(name, type, format, lang, offsetId));
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder lang(String lang) {
            this.lang = lang;
            return this;
        }

        public Builder offsetId(String offsetId) {
            this.offsetId = offsetId;
            return this;
        }
    }

    /**
     * get unix timestamp
     *
     * @param value value
     * @return unix timestamp
     */
    public long getTimeStamp(String value) {
        if (type == Type.LONG) {
            if (value.length() != 13) throw new KRunException(
                    concat(" ", "unix timeStamp", value, "length error..."));
            return Long.valueOf(value);
        }
        long l;
        try {
            l = ZonedDateTime.parse(value, formatter).toInstant().toEpochMilli();
        } catch (DateTimeParseException e1) {
            try {
                l = OffsetDateTime.parse(value, formatter).toInstant().toEpochMilli();
            } catch (DateTimeParseException e2) {
                try {
                    l = LocalDateTime.parse(value, formatter).toEpochSecond(ZoneOffset.of(offsetId)) * 1000L;
                } catch (DateTimeParseException e3) {
                    l = LocalDate.parse(value, formatter).toEpochDay() * 86_400_000L;
                }
            }
        }
        return l;
    }

    /**
     * convert timeValue
     *
     * @param value  source time value
     * @param target ksTime {@link KTime}
     * @return new value
     */
    public String convert(String value, KTime target) {
        if (target.formatter != null) return target.formatter.format(getDateTime(value));
        else return String.valueOf(getTimeStamp(value));
    }

    /**
     * format date value
     * <p>
     * todo 对zoneId之类的有待确认
     *
     * @param value value
     * @return temporalAccessor
     */
    private TemporalAccessor getDateTime(String value) {
        if (type == Type.LONG) {
            if (value.length() != 13) throw new KRunException(
                    concat(" ", "unix timeStamp", value, "length error..."));
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.valueOf(value)),
                    ZoneOffset.of(offsetId));
        }
        TemporalAccessor temporalAccessor;
        try {
            temporalAccessor = ZonedDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            try {
                temporalAccessor = OffsetDateTime.parse(value, formatter);
            } catch (DateTimeParseException e1) {
                try {
                    temporalAccessor = LocalDateTime.parse(value, formatter);
                } catch (DateTimeParseException e2) {
                    temporalAccessor = LocalDate.parse(value, formatter);
                }
            }
        }
        return temporalAccessor;
    }

}
