package com.unimas.kska.integration;

import com.unimas.kska.bean.AppInfo;
import com.unimas.kska.bean.ServiceInfo;
import com.unimas.kska.error.KRunException;
import com.unimas.kska.kafka.KaJMX;
import com.unimas.kska.kafka.KskaClient;
import com.unimas.kska.webservice.MysqlOperator;
import com.unimas.kska.webservice.RegularlyUpdate;
import com.unimas.kska.webservice.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合并代码的server
 */
public class KsServer {
    private static final Logger logger = LoggerFactory.getLogger(com.unimas.kska.KsServer.class);

    private static final String root_dir = System.getProperty("ks.root.dir", "/home/ylzhang/projects/kska");
    public static final Path app_dir = Paths.get(root_dir, "app");
    public static final Path bin_dir = Paths.get(root_dir, "bin");
    public static final Path upload_dir = Paths.get(root_dir, "upload");
    public static final Path download_dir = Paths.get(root_dir, "download");
    public static final ConcurrentHashMap<String, ServiceInfo> caches = new ConcurrentHashMap<>();

    private static MysqlOperator mysqlOperator = null;
    private static KaJMX kaJMX = null;
    private static KskaClient ksKaClient = null;
    private final RegularlyUpdate bst = new RegularlyUpdate();

    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final MethodType clazType = MethodType.methodType(void.class, Class.class, String.class,
            HttpServletRequest.class, HttpServletResponse.class);

    public KsServer() throws Exception {
        mysqlOperator = new MysqlOperator("com.mysql.jdbc.Driver", "10.68.120.184", "3306", "scb"
                , "unimas", "logstash", "1", "3");
//            String jmxUrl = p.getProperty("jmx.url");
//            if (jmxUrl != null && !jmxUrl.isEmpty()) kaJMX = new KaJMX(jmxUrl);
//            else logger.warn("===============kafka jmx url undefined===============");
//            String zkUrl = p.getProperty("zk.url");
//            if (zkUrl != null && !zkUrl.isEmpty()) ksKaClient = new KskaClient(zkUrl);
//            else logger.warn("===============zookeeper url undefined===============");
        if (Files.notExists(app_dir, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(app_dir);
        logger.info("init ks applications");
        this.initApp();
        logger.info("start regularlyUpdate");
        bst.start();
    }

    public void close() {
        try {
            if (mysqlOperator != null) mysqlOperator.close();
            if (kaJMX != null) kaJMX.close();
            if (ksKaClient != null) ksKaClient.close();
            bst.interrupt();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void service(String target, HttpServletRequest request, HttpServletResponse response) {
        String category = target.substring(5, 7);
        String action = target.substring(8);
        char _c = action.charAt(0);
        _c -= 32;
        String name = _c + action.substring(1);
        String pack = "com.unimas.kska.webservice.impl." + category;
        String aidName = pack + "." + "AidHandle";
        String targetName = pack + "." + name;
        String error;
        try {
            Class aidClaz = Class.forName(aidName);
            Class targetClaz = Class.forName(targetName);
            String method = request.getMethod();
            if (method.equalsIgnoreCase("GET")) {
                method = "doGet";
            } else {
                method = "doPost";
            }
            MethodHandle mh = lookup.findVirtual(aidClaz, "handle", clazType);
            mh.invoke(aidClaz.newInstance(), targetClaz, method, request, response);
            return;
        } catch (ClassNotFoundException e) {
            error = targetName + " not found!";
            logger.error(error, e);
        } catch (Throwable throwable) {
            error = "执行请求出错!";
            logger.error(error, throwable);
        }
        try {
            OutputStream outputStream = response.getOutputStream();
            String msg = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(msg.getBytes("utf-8"));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 初始化app信息
     *
     * @throws IOException e
     */
    private void initApp() throws SQLException, IOException {
        List<Map<String, String>> list = mysqlOperator.query(
                "select service_id,service_name,service_desc from ksservice");
        for (Map<String, String> services : list) {
            ServiceInfo serviceInfo = new ServiceInfo();
            String service_id = services.get("service_id");
            List<Map<String, String>> applist = mysqlOperator.query(
                    "select app_id,app_name,app_desc,app_status,zk_url from ksapp where service_id=?",
                    service_id);
            serviceInfo.setName(services.get("service_name"));
            serviceInfo.setDesc(services.get("service_desc"));
            for (Map<String, String> apps : applist) {
                AppInfo appInfo = new AppInfo();
                String app_id = apps.get("app_id");
                appInfo.setId(app_id);
                String app_name = apps.get("app_name");
                appInfo.setName(app_name);
                appInfo.setDesc(apps.get("app_desc"));
                appInfo.setStatus(Integer.parseInt(apps.get("app_status")));
                appInfo.setZkUrl(apps.get("zk_url"));
                if (appInfo.getStatus() == AppInfo.Status.RUN) {
                    File pf = app_dir.resolve(app_id).resolve("pid").toFile();
                    if (pf.exists()) {
                        String pid = com.google.common.io.Files.readFirstLine(pf, Charset.forName("UTF-8"));
                        appInfo.setPid(pid);
                    } else {
                        logger.warn("任务 " + app_name + " pid 文件丢失,状态重置为stop");
                        WSUtils.updateMysqlStatus(app_id, AppInfo.Status.STOP);
                        appInfo.setStatus(AppInfo.Status.STOP);
                    }
                }
                serviceInfo.addAppInfo(appInfo);
            }
            caches.put(service_id, serviceInfo);
        }
    }

    //======================================
    //**************************************
    //======================================
    public static MysqlOperator getMysqlOperator() {
        if (mysqlOperator == null) throw new KRunException("数据库连接为空");
        return mysqlOperator;
    }

    public static KaJMX getKaJMX() {
        if (kaJMX == null) throw new KRunException("jmx连接为空,请重新保存平台kafka");
        return kaJMX;
    }


    public static KskaClient getKsKaClient() {
        if (ksKaClient == null) throw new KRunException("zookeeper连接为空,请重新保存平台kafka");
        return ksKaClient;
    }

    public static void setKaJMX(String url) throws IOException {
        if (kaJMX != null) kaJMX.close();
        kaJMX = new KaJMX(url);
    }

    public static void setKsKaClient(String url) {
        if (ksKaClient != null) ksKaClient.close();
        ksKaClient = new KskaClient(url);
    }

    public static void overWrite(String zkUrl, String jmxUrl) throws IOException {
        Path cf = Paths.get(root_dir, "conf", "server.conf");
        if (cf.toFile().exists()) {
            Properties p = new Properties();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(cf.toFile()), Charset.forName("UTF-8"))) {
                p.load(reader);
            }
            p.put("zk.url", zkUrl);
            p.put("jmx.url", jmxUrl);
            try (FileOutputStream output = new FileOutputStream(cf.toFile())) {
                p.store(output, null);
            }
        }
    }
}
