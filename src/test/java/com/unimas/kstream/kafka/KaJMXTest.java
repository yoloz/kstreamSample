package com.unimas.kstream.kafka;

import com.unimas.kstream.bean.KJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class KaJMXTest {

    private KaJMX kaJMX;

    @Before
    public void setUp() throws Exception {
        kaJMX = new KaJMX("10.68.23.11:9999");
    }

    @After
    public void tearDown() throws Exception {
        kaJMX.close();
    }

    @Test
    public void getLogEndOffset() throws Exception {
        System.out.println(KJson.writeValueAsString(kaJMX.getLogEndOffset("__consumer_offsets")));
    }
}