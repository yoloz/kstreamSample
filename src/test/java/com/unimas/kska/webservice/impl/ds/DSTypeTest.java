package com.unimas.kska.webservice.impl.ds;

import com.unimas.kska.bean.DSType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

//import static org.junit.Assert.*;

public class DSTypeTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getType() {
        System.out.println(DSType.getType("kafka"));
    }
}