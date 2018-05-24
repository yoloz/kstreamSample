package com.unimas.kska.process;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

//import static org.junit.Assert.*;

public class AppImplTest {


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void appTest() {
        CountDownLatch latch = new CountDownLatch(1);
        AppImpl appImpl = new AppImpl("/home/ylzhang/projects/kstream/app/5af008fd1000ad/main.properties",
                latch, false);
        appImpl.run();
        try {
            Thread.sleep(3*60*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}