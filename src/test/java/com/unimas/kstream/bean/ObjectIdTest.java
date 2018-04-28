package com.unimas.kstream.bean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class ObjectIdTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void get() {
        ObjectId id = ObjectId.get();
        System.out.println(id.toString());
        System.out.println(id.getDate());
    }

    @Test
    public void compareTo() {
        String bin = "01011010";
        System.out.println(Integer.parseInt(bin,2));

    }
}