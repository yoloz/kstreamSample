package com.unimas.kstream.bean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


//import static org.junit.Assert.*;

public class ObjectIdTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void get() {
        ObjectId objectId = ObjectId.get();
        String id = objectId.toString();
        System.out.println(id);
        System.out.println(new ObjectId(id).equals(objectId));
    }

    @Test
    public void compareTo() {
        String bin = "01011010";
        int count = Integer.parseInt(bin, 2);
        System.out.println(count);
        System.out.println(Integer.numberOfLeadingZeros(count));

    }
}