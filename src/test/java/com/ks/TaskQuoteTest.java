package com.ks;

import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TaskQuoteTest {


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void startTask() {

    }

    @Test
    public void stopTask() {
    }

    public static void main(String[] args) {
        PropertyConfigurator.configure("/home/ylzhang/projects/bigDataSamples/kstream/config/log4j.properties");
        TaskQuote taskQuote = new TaskQuote("/home/ylzhang/projects/bigDataSamples/kstream/app");
        taskQuote.start();
//        try {
//            Thread.sleep(20 * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        taskQuote.stopTask();
    }
}