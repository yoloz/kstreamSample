/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.unimas.kska.webservice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.unimas.kska.error.KConfigException;
import com.unimas.kska.process.KUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MysqlOperator implements KUtils {

    private final Logger logger = LoggerFactory.getLogger(MysqlOperator.class);

    private BasicDataSource dataSource;

    public MysqlOperator(String driver, String host, String port, String user, String pwd,
                         String dbName, String connMin, String connMax) {
        String url = "jdbc:mysql://" + host + ":" + port +
                "/" + dbName + "?useUnicode=true&characterEncoding=utf-8";
        this.dataSource = new BasicDataSource();
        this.dataSource.setDriverClassName(driver);
        this.dataSource.setUrl(url);
        this.dataSource.setInitialSize(Integer.parseInt(connMin));
        this.dataSource.setMaxTotal(Integer.parseInt(connMax));
        this.dataSource.setMaxIdle(Integer.parseInt(connMax));
        this.dataSource.setMinIdle(Integer.parseInt(connMin));
        this.dataSource.setUsername(user);
        this.dataSource.setPassword(pwd);
        this.dataSource.setDefaultAutoCommit(false);
    }

    public MysqlOperator(Properties properties) {
        if (properties == null || properties.isEmpty()) throw new KConfigException("数据库连接未配置");
        String url = nonNullEmpty(properties, "db.url");
        String user = nonNullEmpty(properties, "db.user");
        String pwd = nonNullEmpty(properties, "db.pwd");
        String _initialSize = properties.getProperty("db.pool.initialSize", "1");
        int initialSize;
        try {
            initialSize = Integer.parseInt(_initialSize);
        } catch (NumberFormatException e) {
            initialSize = 1;
        }
        String _maxTotal = properties.getProperty("db.pool.maxTotal", "4");
        int maxTotal;
        try {
            maxTotal = Integer.parseInt(_maxTotal);
        } catch (NumberFormatException e) {
            maxTotal = 4;
        }
        String _maxIdle = properties.getProperty("db.pool.maxIdle", "4");
        int maxIdle;
        try {
            maxIdle = Integer.parseInt(_maxIdle);
        } catch (NumberFormatException e) {
            maxIdle = 4;
        }
        String _minIdle = properties.getProperty("db.pool.minIdle", "0");
        int minIdle;
        try {
            minIdle = Integer.parseInt(_minIdle);
        } catch (NumberFormatException e) {
            minIdle = 0;
        }
        this.dataSource = new BasicDataSource();
        this.dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        this.dataSource.setUrl(url);
        this.dataSource.setInitialSize(initialSize);
        this.dataSource.setMaxTotal(maxTotal);
        this.dataSource.setMaxIdle(maxIdle);
        this.dataSource.setMinIdle(minIdle);
        this.dataSource.setUsername(user);
        this.dataSource.setPassword(pwd);
        this.dataSource.setDefaultAutoCommit(false);
    }

    /**
     * 不确定是插入还是更新,统一先删除再插入,同时更新服务的运行状态
     *
     * @param status app status sql
     * @param delete delete sql
     * @param sql    fixed  sql
     * @param param  insert data
     * @throws SQLException e
     */
    public void update(String status, String delete, String sql, Object... param) throws SQLException {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            PreparedStatement dps = null;
            if (delete != null) {
                dps = conn.prepareStatement(delete);
                dps.executeUpdate();
            }
            PreparedStatement ips = conn.prepareStatement(sql);
            for (int i = 0; i < param.length; i++) {
                ips.setObject(i + 1, param[i]);
            }
            ips.executeUpdate();
            PreparedStatement sps = null;
            if (status != null) {
                sps = conn.prepareStatement(status);
                sps.executeUpdate();
            }
            conn.commit();
            if (dps != null) dps.close();
            ips.close();
            if (sps != null) sps.close();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    /**
     * 不确定是插入还是更新,统一先删除再插入
     *
     * @param delete delete sql
     * @param sql    fixed sql
     * @param param  insert data
     * @throws SQLException e
     */
    public void update(String delete, String sql, Object... param) throws SQLException {
        this.update(null, delete, sql, param);
    }

    /**
     * 插入操作及确定的更新操作
     *
     * @param sql   sql
     * @param param insert or update data
     * @throws SQLException e
     */
    public void fixUpdate(String sql, Object... param) throws SQLException {
        this.update(null, null, sql, param);
    }

    public List<Map<String, String>> query(String sql, Object... param) throws SQLException {
        List<Map<String, String>> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < param.length; i++) {
                ps.setObject(i + 1, param[i]);
            }
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            while (rs.next()) {
                int count = metaData.getColumnCount();
                Map<String, String> map = new HashMap<>(count);
                for (int i = 1; i <= count; i++) {
                    map.put(metaData.getColumnName(i), rs.getString(i));
                }
                list.add(map);
            }
            rs.close();
        }
        return list;
    }

    public boolean exist(String sql, Object... param) throws SQLException {
        boolean result;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < param.length; i++) {
                ps.setObject(i + 1, param[i]);
            }
            ResultSet rs = ps.executeQuery();
            result = rs.next();
            rs.close();
        }
        return result;
    }

    public void close() {
        try {
            if (this.dataSource != null) this.dataSource.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
