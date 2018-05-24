package com.unimas.kska.bean;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.junit.Test;


/**
 *
 */
public class KTimeTest {

    private ImmutableList<String> stringValues = ImmutableList.of("2017-11-24 15:20:35.000Z",
            "2017-11-24 15:20:35.000", "2017-11-24 15:20:35", "2017-11-24");
    private ImmutableList<String> longValues = ImmutableList.of("1511536835000", "1511536835000",
            "1511536835000", "1511481600000");
    private ImmutableList<String> formats = ImmutableList.of("uuuu-MM-dd HH:mm:ss.SSSX",
            "uuuu-MM-dd HH:mm:ss.SSS", "uuuu-MM-dd HH:mm:ss", "uuuu-MM-dd");

    @Test
    public void getTimeStamp() {
        KTime.Builder builder = new KTime.Builder().name("test").offsetId("+00:00").lang("en");
        for (int i = 0; i < stringValues.size(); i++) {
            Optional<KTime> optional = builder.type("string").format(formats.get(i)).build();
            if (optional.isPresent()) System.out.println(optional.get().getTimeStamp(stringValues.get(i)));
        }
    }

    @Test
    public void convert() {
        KTime.Builder builder = new KTime.Builder().name("source").offsetId("+00:00").lang("en");
        KTime.Builder otherB = new KTime.Builder().name("other").offsetId("+00:00").lang("en");
        for (int i = 0; i < longValues.size(); i++) {
            Optional<KTime> optional = builder.type("long").build();
            Optional<KTime> other = otherB.format(formats.get(i)).build();
            if (optional.isPresent() && other.isPresent())
                System.out.println(optional.get().convert(longValues.get(i), other.get()));
        }
    }
}