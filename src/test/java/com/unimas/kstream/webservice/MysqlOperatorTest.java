package com.unimas.kstream.webservice;

import com.google.gson.reflect.TypeToken;
import com.unimas.kstream.bean.KJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//import static org.junit.Assert.*;

public class MysqlOperatorTest {

    MysqlOperator mysqlOperator;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        properties.put("db.url", "jdbc:mysql://10.68.120.184:3306/logstash?useUnicode=true&characterEncoding=utf-8");
        properties.put("db.user", "scb");
        properties.put("db.pwd", "unimas");
        mysqlOperator = new MysqlOperator(properties);
    }

    @After
    public void tearDown() throws Exception {
        mysqlOperator.close();
    }

    @Test
    public void query() throws SQLException, IOException {
        List<Map<String, String>> result = mysqlOperator.query("select * from ksservice");
        System.out.println(KJson.writeValue(result, new TypeToken<List<Map<String, String>>>() {
        }.getType()));
    }
}