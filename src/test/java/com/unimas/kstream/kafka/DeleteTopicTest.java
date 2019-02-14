package com.unimas.kstream.kafka;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DeleteTopicTest {

    DeleteTopic deleteTopic;
    @Before
    public void setUp() throws Exception {
        deleteTopic = new DeleteTopic("10.68.23.11:2181","temp");
    }

    @Test
    public void testRun(){
        deleteTopic.run();
    }
    @After
    public void tearDown() throws Exception {
    }
}